// register-face.js
const video = document.getElementById("video");
const canvas = document.getElementById("overlay");
const resultDiv = document.getElementById("result");
const username = document.body.dataset.username;
let hasRegistered = false;

Promise.all([
    faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
    faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
    faceapi.nets.faceRecognitionNet.loadFromUri("/models")
]).then(startVideo);

function startVideo() {
    navigator.mediaDevices.getUserMedia({ video: true })
        .then((stream) => {
            video.srcObject = stream;
            video.onloadedmetadata = () => {
                video.play();
                startFaceDetection();
            };
        })
        .catch((err) => {
            console.error("Không thể truy cập camera", err);
            showError("❌ Không thể truy cập camera.");
        });
}

function stopVideoStream() {
    const stream = video.srcObject;
    if (stream) {
        stream.getTracks().forEach(track => track.stop());
        video.srcObject = null;
    }
}

function showSuccess(message) {
    resultDiv.textContent = message;
    resultDiv.className = "text-success fw-bold mt-3";
}

function showError(message) {
    resultDiv.textContent = message;
    resultDiv.className = "text-danger fw-bold mt-3";
}

function startFaceDetection() {
    const displaySize = { width: video.width, height: video.height };
    faceapi.matchDimensions(canvas, displaySize);
    const context = canvas.getContext("2d");

    const interval = setInterval(async () => {
        if (hasRegistered) return;

        const detection = await faceapi
            .detectSingleFace(video, new faceapi.TinyFaceDetectorOptions())
            .withFaceLandmarks()
            .withFaceDescriptor();

        context.clearRect(0, 0, canvas.width, canvas.height);

        if (detection) {
            const { x, y, width, height } = detection.detection.box;
            const centerX = x + width / 2;
            const centerY = y + height / 2;
            const radius = Math.max(width, height) / 2;

            context.beginPath();
            context.arc(centerX, centerY, radius, 0, 2 * Math.PI);
            context.lineWidth = 4;
            context.strokeStyle = "#0d6efd";
            context.shadowColor = "#0d6efd";
            context.shadowBlur = 20;
            context.stroke();
            context.shadowBlur = 0;

            hasRegistered = true;
            showSuccess("📤 Đang lưu khuôn mặt...");

            const descriptor = Array.from(detection.descriptor);
            try {
                const response = await fetch("/api/register-descriptor", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ username, descriptor })
                });

                if (response.ok) {
                    showSuccess("✅ Đăng ký thành công! Đang chuyển trang...");
                    stopVideoStream();
                    clearInterval(interval);
                    setTimeout(() => {
                        window.location.href = "/acvstore/verify-face";
                    }, 1500);
                } else {
                    throw new Error("Lỗi khi lưu descriptor");
                }
            } catch (err) {
                console.error(err);
                showError("❌ Không thể lưu khuôn mặt. Vui lòng thử lại.");
                hasRegistered = false;
            }
        }
    }, 1000);
}
