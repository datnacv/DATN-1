// register-face.js
const video = document.getElementById("video");
const canvas = document.getElementById("overlay");
const resultDiv = document.getElementById("result");
const username = document.body.dataset.username;

let hasRegistered = false;
let stableCount = 0;
const STABLE_NEED = 5; // số khung hình ổn định liên tiếp

// Vẽ vòng tròn hướng dẫn ở giữa khung
function drawGuide(ctx, W, H) {
    const r = Math.min(W, H) * 0.28; // bán kính vòng hướng dẫn
    const cx = W / 2, cy = H / 2;

    ctx.save();
    ctx.clearRect(0, 0, W, H);

    // che mờ ngoài vòng (mask)
    ctx.fillStyle = "rgba(0,0,0,0.35)";
    ctx.beginPath();
    ctx.rect(0, 0, W, H);
    ctx.arc(cx, cy, r, 0, Math.PI * 2, true);
    ctx.closePath();
    ctx.fill("evenodd");

    // viền vòng
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.setLineDash([6, 8]);
    ctx.lineWidth = 3;
    ctx.strokeStyle = "rgba(255,255,255,.9)";
    ctx.shadowColor = "rgba(13,110,253,.9)";
    ctx.shadowBlur = 10;
    ctx.stroke();

    ctx.restore();
    return { cx, cy, r };
}

function showMsg(msg, type = "info") {
    const map = {
        info: "text-secondary",
        warn: "text-warning",
        error: "text-danger",
        ok: "text-success"
    };
    resultDiv.className = `${map[type] || map.info} mt-3 fw-medium`;
    resultDiv.textContent = msg;
}

Promise.all([
    faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
    faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
    faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
]).then(startVideo).catch(e=>{
    console.error(e);
    showMsg("❌ Không tải được mô hình nhận diện.", "error");
});

function startVideo() {
    navigator.mediaDevices.getUserMedia({ video: { facingMode: "user" }, audio: false })
        .then(stream => {
            video.srcObject = stream;
            video.onloadedmetadata = () => {
                video.play();
                startDetectLoop();
            };
        })
        .catch(err => {
            console.error("Không thể truy cập camera", err);
            showMsg("❌ Không thể truy cập camera.", "error");
        });
}

function stopVideoStream() {
    const stream = video.srcObject;
    if (stream) {
        stream.getTracks().forEach(t => t.stop());
        video.srcObject = null;
    }
}

async function startDetectLoop() {
    const ctx = canvas.getContext("2d");
    const W = canvas.width = video.videoWidth || video.clientWidth;
    const H = canvas.height = video.videoHeight || video.clientHeight;

    const guide = drawGuide(ctx, W, H);

    const tick = async () => {
        if (hasRegistered) return;

        // luôn vẽ lại hướng dẫn
        drawGuide(ctx, W, H);

        // phát hiện tất cả khuôn mặt
        const detections = await faceapi
            .detectAllFaces(video, new faceapi.TinyFaceDetectorOptions())
            .withFaceLandmarks()
            .withFaceDescriptors();

        // nhiều mặt
        if (detections.length > 1) {
            stableCount = 0;
            showMsg("⚠️ Có nhiều khuôn mặt. Chỉ một người đứng trước camera.", "warn");
            requestAnimationFrame(tick);
            return;
        }

        // không có mặt
        if (detections.length === 0) {
            stableCount = 0;
            showMsg("👋 Đưa mặt vào giữa vòng tròn.", "info");
            requestAnimationFrame(tick);
            return;
        }

        // đúng 1 mặt
        const det = detections[0];
        const box = det.detection.box;
        const faceCx = box.x + box.width / 2;
        const faceCy = box.y + box.height / 2;
        const faceR  = Math.max(box.width, box.height) / 2;

        // điều kiện nằm gọn trong vòng tròn
        const dist = Math.hypot(faceCx - guide.cx, faceCy - guide.cy);
        const inside = dist + faceR <= guide.r * 0.98; // chừa biên 2%

        // ràng buộc gần/xa hợp lý
        const minR = guide.r * 0.45; // quá nhỏ => đứng xa
        const maxR = guide.r * 0.80; // quá lớn => đứng gần
        const sizeOk = faceR >= minR && faceR <= maxR;

        // vẽ khung mặt
        ctx.beginPath();
        ctx.arc(faceCx, faceCy, faceR, 0, Math.PI * 2);
        ctx.lineWidth = 2;
        ctx.setLineDash([]);
        ctx.strokeStyle = inside && sizeOk ? "#22c55e" : "#ef4444";
        ctx.shadowColor = ctx.strokeStyle;
        ctx.shadowBlur = 12;
        ctx.stroke();

        if (!inside) {
            stableCount = 0;
            showMsg("📍 Căn giữa khuôn mặt vào vòng tròn.", "warn");
            requestAnimationFrame(tick);
            return;
        }

        if (!sizeOk) {
            stableCount = 0;
            showMsg(faceR < minR ? "🔍 Tiến gần thêm một chút." : "↔️ Lùi ra xa một chút.", "warn");
            requestAnimationFrame(tick);
            return;
        }

        // ổn định
        stableCount++;
        showMsg(`✅ Giữ yên… (${stableCount}/${STABLE_NEED})`, "ok");

        if (stableCount >= STABLE_NEED) {
            await registerDescriptor(det.descriptor);
            return; // dừng loop
        }

        requestAnimationFrame(tick);
    };

    requestAnimationFrame(tick);
}

async function registerDescriptor(descriptor) {
    try {
        hasRegistered = true;
        showMsg("📤 Đang lưu khuôn mặt…", "ok");

        const response = await fetch("/api/register-descriptor", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, descriptor: Array.from(descriptor) })
        });

        if (!response.ok) throw new Error("Lỗi khi lưu descriptor");

        showMsg("🎉 Đăng ký thành công! Đang chuyển trang…", "ok");
        stopVideoStream();
        setTimeout(() => (window.location.href = "/acvstore/verify-face"), 1200);
    } catch (e) {
        console.error(e);
        hasRegistered = false;
        showMsg("❌ Không thể lưu khuôn mặt. Thử lại nhé.", "error");
    }
}
