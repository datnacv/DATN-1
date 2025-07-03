document.addEventListener("DOMContentLoaded", async () => {
    const video = document.getElementById("video");
    const resultDiv = document.getElementById("result");
    const username = "admin";

    console.log("🚀 Starting face verification for:", username);

    try {
        resultDiv.textContent = "Đang tải models...";
        await Promise.all([
            faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
            faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
            faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
        ]);
        console.log("✅ Đã load xong models");

        resultDiv.textContent = "Đang khởi động camera...";
        const stream = await navigator.mediaDevices.getUserMedia({ video: {} });
        video.srcObject = stream;
        console.log("✅ Camera started successfully");
    } catch (e) {
        console.error("❌ Lỗi khi load models hoặc camera:", e);
        resultDiv.textContent = "Lỗi: " + e.message;
        return;
    }

    video.addEventListener("play", async () => {
        console.log("📹 Video started playing");

        const canvas = document.getElementById("overlay");
        const displaySize = { width: 600, height: 450 };
        faceapi.matchDimensions(canvas, displaySize);

        try {
            resultDiv.textContent = "Đang tải dữ liệu khuôn mặt...";
            const descriptorResponse = await fetch(`/api/get-descriptor?username=${encodeURIComponent(username)}`);

            if (!descriptorResponse.ok) {
                const errorData = await descriptorResponse.json().catch(() => ({}));
                throw new Error(`Server error: ${descriptorResponse.status} - ${errorData.message || 'Unknown error'}`);
            }

            const descriptorData = await descriptorResponse.json();
            if (!descriptorData.descriptors || !Array.isArray(descriptorData.descriptors) || descriptorData.descriptors.length === 0) {
                throw new Error("Invalid descriptor data format");
            }

            const labeledDescriptors = new faceapi.LabeledFaceDescriptors(
                username,
                descriptorData.descriptors.map(d => new Float32Array(d))
            );
            const faceMatcher = new faceapi.FaceMatcher([labeledDescriptors], 0.6);

            resultDiv.textContent = "Đang nhận diện khuôn mặt...";

            let verificationAttempts = 0;
            const maxAttempts = 3;
            let consecutiveMatches = 0;
            let stopDetection = false;

            const detectFace = async () => {
                if (stopDetection) return;

                const detections = await faceapi
                    .detectAllFaces(video, new faceapi.TinyFaceDetectorOptions())
                    .withFaceLandmarks()
                    .withFaceDescriptors();

                const resizedDetections = faceapi.resizeResults(detections, displaySize);
                const ctx = canvas.getContext("2d");
                ctx.clearRect(0, 0, canvas.width, canvas.height);

                if (detections.length === 0) {
                    resultDiv.textContent = "Không phát hiện khuôn mặt. Vui lòng đối mặt với camera.";
                    consecutiveMatches = 0;
                    requestAnimationFrame(detectFace);
                    return;
                }

                const results = resizedDetections.map(d => faceMatcher.findBestMatch(d.descriptor));
                let foundMatch = false;

                results.forEach((result, i) => {
                    const box = resizedDetections[i].detection.box;
                    const isMatch = result.label === username && result.distance < 0.6;
                    const drawBox = new faceapi.draw.DrawBox(box, {
                        label: `${result.label} (${result.distance.toFixed(2)})`,
                        boxColor: isMatch ? '#00ff00' : '#ff0000'
                    });
                    drawBox.draw(canvas);

                    if (isMatch) {
                        foundMatch = true;
                        consecutiveMatches++;
                        resultDiv.textContent = `Đã nhận diện: ${result.label} (${consecutiveMatches}/3)`;

                        if (consecutiveMatches >= 3) {
                            stopDetection = true;
                            resultDiv.textContent = "✅ Xác minh thành công! Đang chuyển hướng...";

                            fetch("/acvstore/verify-success", { method: "POST" })
                                .then(res => res.json())
                                .then(data => {
                                    if (data.success) {
                                        setTimeout(() => {
                                            window.location.href = "/acvstore/thong-ke";
                                        }, 1000);
                                    } else {
                                        resultDiv.textContent = "Lỗi xác minh. Vui lòng thử lại.";
                                    }
                                })
                                .catch(err => {
                                    console.error("❌ Lỗi gửi xác minh:", err);
                                    resultDiv.textContent = "Lỗi kết nối. Vui lòng thử lại.";
                                });
                        }
                    }
                });

                if (!foundMatch) {
                    consecutiveMatches = 0;
                    verificationAttempts++;
                    resultDiv.textContent = `Khuôn mặt không khớp. Thử lại... (${verificationAttempts}/${maxAttempts})`;

                    if (verificationAttempts >= maxAttempts) {
                        stopDetection = true;
                        resultDiv.textContent = "❌ Xác minh thất bại. Vui lòng liên hệ quản trị viên.";
                    }
                }

                if (!stopDetection) requestAnimationFrame(detectFace);
            };

            detectFace();

        } catch (error) {
            console.error("❌ Lỗi tải descriptor:", error);
            resultDiv.textContent = `Lỗi: ${error.message}`;
        }
    });
});
