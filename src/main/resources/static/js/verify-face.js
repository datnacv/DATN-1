document.addEventListener("DOMContentLoaded", async () => {
    const video = document.getElementById("video");
    const resultDiv = document.getElementById("result");
    const username = "admin"; // 👈 Bạn có thể truyền động từ backend xuống thay vì hardcode

    console.log("🚀 Bắt đầu xác minh khuôn mặt cho:", username);

    try {
        resultDiv.textContent = "⏳ Đang tải models...";
        await Promise.all([
            faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
            faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
            faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
        ]);
        console.log("✅ Đã load models thành công");

        resultDiv.textContent = "⏳ Đang mở camera...";
        const stream = await navigator.mediaDevices.getUserMedia({ video: {} });
        video.srcObject = stream;
        console.log("📷 Camera đã hoạt động");

    } catch (e) {
        console.error("❌ Lỗi khi khởi tạo camera/models:", e);
        resultDiv.textContent = "Lỗi: " + e.message;
        return;
    }

    video.addEventListener("play", async () => {
        const canvas = document.getElementById("overlay");
        const displaySize = { width: 600, height: 450 };
        faceapi.matchDimensions(canvas, displaySize);

        try {
            resultDiv.textContent = "⏳ Đang lấy dữ liệu khuôn mặt từ server...";
            const response = await fetch(`/api/get-descriptor?username=${encodeURIComponent(username)}`);
            const data = await response.json();

            if (!response.ok || !data.descriptors || !Array.isArray(data.descriptors) || data.descriptors.length === 0) {
                throw new Error(data.message || "Không tìm thấy dữ liệu khuôn mặt");
            }

            const labeledDescriptor = new faceapi.LabeledFaceDescriptors(
                username,
                data.descriptors.map(d => new Float32Array(d))
            );
            const faceMatcher = new faceapi.FaceMatcher([labeledDescriptor], 0.6);

            resultDiv.textContent = "👁️ Đang xác minh khuôn mặt...";

            let consecutiveMatches = 0;
            const requiredMatches = 3;
            let attempts = 0;
            const maxAttempts = 5;
            let stop = false;

            const detectLoop = async () => {
                if (stop) return;

                const detections = await faceapi
                    .detectAllFaces(video, new faceapi.TinyFaceDetectorOptions())
                    .withFaceLandmarks()
                    .withFaceDescriptors();

                const resized = faceapi.resizeResults(detections, displaySize);
                const ctx = canvas.getContext("2d");
                ctx.clearRect(0, 0, canvas.width, canvas.height);

                if (detections.length === 0) {
                    resultDiv.textContent = "😶 Không phát hiện khuôn mặt. Vui lòng đối diện camera.";
                    consecutiveMatches = 0;
                    requestAnimationFrame(detectLoop);
                    return;
                }

                const results = resized.map(d => faceMatcher.findBestMatch(d.descriptor));
                let matched = false;

                results.forEach((result, i) => {
                    const box = resized[i].detection.box;
                    const match = result.label === username && result.distance < 0.6;

                    const drawBox = new faceapi.draw.DrawBox(box, {
                        label: match ? `✅ ${result.label} (${result.distance.toFixed(2)})` : `❌ ${result.label}`,
                        boxColor: match ? "#00ff00" : "#ff0000"
                    });
                    drawBox.draw(canvas);

                    if (match) {
                        matched = true;
                        consecutiveMatches++;
                        resultDiv.textContent = `✅ Đã khớp (${consecutiveMatches}/${requiredMatches})`;

                        if (consecutiveMatches >= requiredMatches) {
                            stop = true;
                            resultDiv.textContent = "🎉 Xác minh thành công!";

                            // Gửi xác minh lên server
                            fetch("/acvstore/verify-success", { method: "POST" })
                                .then(res => res.json())
                                .then(res => {
                                    if (res.success) {
                                        resultDiv.textContent = "✅ Đã xác minh. Chuyển hướng...";
                                        setTimeout(() => window.location.href = "/acvstore/thong-ke", 1000);
                                    } else {
                                        resultDiv.textContent = "❌ Xác minh thất bại!";
                                    }
                                })
                                .catch(err => {
                                    console.error("Lỗi gửi xác minh:", err);
                                    resultDiv.textContent = "❌ Lỗi xác minh session.";
                                });
                        }
                    }
                });

                if (!matched) {
                    consecutiveMatches = 0;
                    attempts++;
                    resultDiv.textContent = `⚠️ Không khớp. Thử lại... (${attempts}/${maxAttempts})`;

                    if (attempts >= maxAttempts) {
                        stop = true;
                        resultDiv.textContent = "❌ Xác minh thất bại. Vui lòng thử lại hoặc liên hệ quản trị viên.";
                    }
                }

                if (!stop) requestAnimationFrame(detectLoop);
            };

            detectLoop();

        } catch (err) {
            console.error("❌ Lỗi khi tải descriptor:", err);
            resultDiv.textContent = "❌ " + (err.message || "Lỗi không xác định");
        }
    });
});
