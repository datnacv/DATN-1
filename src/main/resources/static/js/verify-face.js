document.addEventListener("DOMContentLoaded", async () => {
    const video = document.getElementById("video");
    const resultDiv = document.getElementById("result");
    const canvas = document.getElementById("overlay");
    const displaySize = { width: 600, height: 450 };
    canvas.width = displaySize.width;
    canvas.height = displaySize.height;

    const username = document.body.getAttribute("data-username");


    console.log("🚀 Bắt đầu xác minh khuôn mặt cho:", username);

    try {
        resultDiv.textContent = "⏳ Đang tải models...";
        await Promise.all([
            faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
            faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
            faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
        ]);
        console.log("✅ Models đã được tải");

        resultDiv.textContent = "⏳ Đang mở camera...";
        const stream = await navigator.mediaDevices.getUserMedia({ video: {} });
        video.srcObject = stream;
    } catch (e) {
        console.error("❌ Lỗi khởi tạo:", e);
        resultDiv.textContent = "Lỗi: " + e.message;
        return;
    }

    video.addEventListener("play", async () => {
        faceapi.matchDimensions(canvas, displaySize);

        try {
            resultDiv.textContent = "⏳ Đang lấy dữ liệu khuôn mặt...";
            const res = await fetch(`/api/get-descriptor?username=${encodeURIComponent(username)}`);
            const data = await res.json();

            if (!res.ok || !data.descriptors || !Array.isArray(data.descriptors) || data.descriptors.length === 0) {
                throw new Error(data.message || "Không có dữ liệu khuôn mặt");
            }

            const labeledDescriptor = new faceapi.LabeledFaceDescriptors(
                username,
                data.descriptors.map(d => new Float32Array(d))
            );
            const faceMatcher = new faceapi.FaceMatcher([labeledDescriptor], 0.45); // 👈 Độ chính xác cao hơn

            resultDiv.textContent = "👁️ Đang xác minh khuôn mặt...";

            let consecutiveMatches = 0;
            const requiredMatches = 5;

            const detectLoop = async () => {
                const detections = await faceapi
                    .detectAllFaces(video, new faceapi.TinyFaceDetectorOptions())
                    .withFaceLandmarks()
                    .withFaceDescriptors();

                const resized = faceapi.resizeResults(detections, displaySize);
                const ctx = canvas.getContext("2d");
                ctx.clearRect(0, 0, canvas.width, canvas.height);

                if (detections.length === 0) {
                    resultDiv.textContent = "😶 Không phát hiện khuôn mặt. Vui lòng nhìn vào camera.";
                    consecutiveMatches = 0;
                    requestAnimationFrame(detectLoop);
                    return;
                }

                let matched = false;

                resized.forEach((detection, i) => {
                    const result = faceMatcher.findBestMatch(detection.descriptor);
                    const box = detection.detection.box;
                    const match = result.label === username && result.distance < 0.45;

                    const drawBox = new faceapi.draw.DrawBox(box, {
                        label: match ? `✅ ${result.label} (${result.distance.toFixed(2)})` : `❌ ${result.label}`,
                        boxColor: match ? "#00ff00" : "#ff0000"
                    });
                    drawBox.draw(canvas);

                    if (match) {
                        matched = true;
                        consecutiveMatches++;
                        resultDiv.textContent = `✅ Đã khớp (${consecutiveMatches}/${requiredMatches})`;
                    }
                });

                if (matched) {
                    if (consecutiveMatches >= requiredMatches) {
                        resultDiv.textContent = "🎉 Xác minh thành công! Đang chuyển hướng...";
                        await fetch("/acvstore/verify-success", {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({ username }),
                            credentials: "include"
                        }).then(res => res.json())
                            .then(data => {
                                if (data.success) {
                                    setTimeout(() => {
                                        window.location.href = "/acvstore/thong-ke";
                                    }, 1000);
                                } else {
                                    resultDiv.textContent = "❌ Xác minh thất bại: " + data.message;
                                    consecutiveMatches = 0;
                                }
                            }).catch(err => {
                                console.error("Lỗi gửi xác minh:", err);
                                resultDiv.textContent = "❌ Gửi xác minh thất bại.";
                            });
                        return;
                    }
                } else {
                    consecutiveMatches = 0;
                    resultDiv.textContent = "⚠️ Không khớp. Đang chờ khuôn mặt hợp lệ...";
                }

                requestAnimationFrame(detectLoop); // tiếp tục quét liên tục
            };

            detectLoop();

        } catch (err) {
            console.error("❌ Lỗi tải dữ liệu:", err);
            resultDiv.textContent = "❌ " + (err.message || "Lỗi không xác định");
        }
    });
});
