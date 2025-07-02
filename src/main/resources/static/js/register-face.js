const video = document.getElementById('video');
const canvas = document.getElementById('overlay');
const resultDiv = document.getElementById('result');

// Lấy username từ Thymeleaf hoặc URL
const urlParams = new URLSearchParams(window.location.search);
const username = urlParams.get('username') || (document.querySelector('body').dataset.username || 'unknown');

let isRegistered = false;

// Load face-api models
Promise.all([
    faceapi.nets.tinyFaceDetector.load('/models'),
    faceapi.nets.faceLandmark68Net.load('/models'),
    faceapi.nets.faceRecognitionNet.load('/models')
])
    .then(startVideo)
    .catch(err => {
        console.error("Lỗi tải model:", err);
        resultDiv.innerHTML = `<span style="color: red">❌ Lỗi tải model nhận diện</span>`;
    });

function startVideo() {
    navigator.mediaDevices.getUserMedia({ video: true })
        .then(stream => {
            video.srcObject = stream;
        })
        .catch(err => {
            console.error("Không thể mở webcam:", err);
            resultDiv.innerHTML = `<span style="color: red">❌ Không thể mở webcam</span>`;
        });
}

video.addEventListener('play', () => {
    const displaySize = { width: video.videoWidth, height: video.videoHeight };
    faceapi.matchDimensions(canvas, displaySize);
    const ctx = canvas.getContext('2d');

    let lastProcessed = 0;
    const targetFPS = 15;
    const frameInterval = 1000 / targetFPS;

    async function detectAndRegister(timestamp) {
        if (isRegistered) return;

        if (timestamp - lastProcessed < frameInterval) {
            requestAnimationFrame(detectAndRegister);
            return;
        }
        lastProcessed = timestamp;

        const detection = await faceapi
            .detectSingleFace(video, new faceapi.TinyFaceDetectorOptions())
            .withFaceLandmarks()
            .withFaceDescriptor();

        ctx.clearRect(0, 0, canvas.width, canvas.height);

        if (detection && detection.descriptor) {
            const resized = faceapi.resizeResults(detection, displaySize);
            faceapi.draw.drawDetections(canvas, resized);
            faceapi.draw.drawFaceLandmarks(canvas, resized);

            resultDiv.innerHTML = `<span style="color: blue">Đang xử lý khuôn mặt...</span>`;

            try {
                const response = await fetch('/api/register-descriptor', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: username,
                        descriptor: Array.from(detection.descriptor)
                    })
                });

                const data = await response.json();
                if (data.success) {
                    resultDiv.innerHTML = `<span style="color: green">✅ ${data.message}</span>`;
                    isRegistered = true;
                    setTimeout(() => {
                        window.location.href = "/acvstore/login";
                    }, 2000);
                } else {
                    resultDiv.innerHTML = `<span style="color: red">❌ ${data.message || 'Đăng ký thất bại'}</span>`;
                }
            } catch (err) {
                console.error("Lỗi gửi descriptor:", err);
                resultDiv.innerHTML = `<span style="color: red">❌ Gửi dữ liệu thất bại</span>`;
            }
        } else {
            resultDiv.innerHTML = `<span style="color: orange">Không phát hiện khuôn mặt</span>`;
        }

        if (!isRegistered) {
            requestAnimationFrame(detectAndRegister);
        }
    }

    requestAnimationFrame(detectAndRegister);
});
