// ===== Helpers =====
function toVND(value) {
    const n = Number(String(value).replace(/[^\d.-]/g, ''));
    return Number.isFinite(n) && n > 0 ? n.toLocaleString('vi-VN') + ' VNĐ' : 'Liên hệ';
}

// ===== DOM refs =====
const form = document.querySelector('form[action="/search"]');
const searchInput = document.getElementById('searchInput');
const searchSuggestions = document.getElementById('searchSuggestions');

// === tránh bind trùng nếu script bị include 2 lần ===
if (form && !form.dataset.init) {
    form.dataset.init = '1';

    // --- Suggestions (giữ logic gốc) ---
    let debounceTimer;
    if (searchInput && searchSuggestions) {
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(async () => {
                const keyword = searchInput.value.trim();
                searchSuggestions.innerHTML = '';

                if (keyword.length < 2) {
                    showSearchHistory(keyword);
                    return;
                }

                try {
                    const response = await fetch(`/api/search?keyword=${encodeURIComponent(keyword)}`, { credentials: 'include' });
                    const products = await response.json();

                    searchSuggestions.innerHTML = '';
                    if (!Array.isArray(products) || products.length === 0) {
                        searchSuggestions.innerHTML = '<div class="suggestion-item">Không tìm thấy sản phẩm, xem các sản phẩm nổi bật</div>';
                    } else {
                        products.forEach((product) => {
                            const priceText = toVND(product.price);
                            const suggestion = document.createElement('div');
                            suggestion.className = 'suggestion-item';
                            suggestion.innerHTML = `
                <img src="${product.urlHinhAnh || '/images/placeholder.png'}" alt="${product.tenSanPham}"/>
                <div>
                  <div class="font-semibold">${product.tenSanPham}</div>
                  <div class="text-blue-800">${priceText}</div>
                </div>`;
                            suggestion.addEventListener('click', () => {
                                window.location.href = `/chitietsanpham?id=${product.id}`;
                            });
                            searchSuggestions.appendChild(suggestion);
                        });
                    }
                    searchSuggestions.classList.add('active');
                } catch (error) {
                    console.error('Error fetching search suggestions:', error);
                    searchSuggestions.innerHTML = '<div class="suggestion-item">Lỗi khi tải gợi ý</div>';
                    searchSuggestions.classList.add('active');
                }
            }, 300);
        });

        // ⚠️ bỏ keydown Enter cũ để không bắn POST 2 lần
        // Thay bằng submit handler ở dưới.

        // focus => show history
        searchInput.addEventListener('focus', () => {
            showSearchHistory(searchInput.value.trim());
        });

        // click outside => close
        document.addEventListener('click', (e) => {
            if (!searchInput.contains(e.target) && !searchSuggestions.contains(e.target)) {
                searchSuggestions.classList.remove('active');
            }
        });
    }

    // --- Submit form: save history 1 lần, rồi điều hướng ---
    form.addEventListener('submit', (e) => {
        const kw = (searchInput?.value || '').trim();
        if (!kw) return; // cho submit tiếp, server sẽ xử lý

        // Anti-spam trong 2s nếu user bấm loạn
        const now = Date.now();
        if (window.__lastSaveKw === kw && now - (window.__lastSaveAt || 0) < 2000) {
            return; // vẫn cho submit, chỉ không bắn POST nữa
        }
        window.__lastSaveKw = kw;
        window.__lastSaveAt = now;

        // Không chặn submit; dùng sendBeacon/fire-and-forget để không delay điều hướng
        const url = `/api/save-search-history?keyword=${encodeURIComponent(kw)}`;
        if (navigator.sendBeacon) {
            const blob = new Blob([], { type: 'application/json' });
            navigator.sendBeacon(url, blob);
        } else {
            // keepalive để vẫn gửi khi đang chuyển trang
            fetch(url, { method: 'POST', keepalive: true, credentials: 'include' }).catch(() => {});
        }
        // không preventDefault => form vẫn GET /search như bình thường
    });
}

// ===== History =====
async function showSearchHistory(filterKeyword = '') {
    if (!searchSuggestions) return;

    try {
        const response = await fetch('/api/search-history', { credentials: 'include' });
        if (!response.ok) {
            throw new Error(response.status === 401 ? 'Vui lòng đăng nhập để xem lịch sử tìm kiếm' : 'Lỗi khi tải lịch sử tìm kiếm');
        }

        const history = await response.json();
        searchSuggestions.innerHTML = '';

        const filtered = filterKeyword
            ? history.filter((kw) => kw.toLowerCase().includes(filterKeyword.toLowerCase()))
            : history;

        if (!Array.isArray(filtered) || filtered.length === 0) {
            searchSuggestions.innerHTML = '<div class="history-item">Không có lịch sử tìm kiếm</div>';
        } else {
            const safe = filterKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

            filtered.forEach((keyword) => {
                const row = document.createElement('div');
                row.className = 'history-item';
                row.innerHTML = `
          <span class="keyword">${
                    filterKeyword ? keyword.replace(new RegExp(safe, 'gi'), (m) => `<span class="highlight">${m}</span>`) : keyword
                }</span>
          <i class="fas fa-times delete-btn" title="Xóa mục này"></i>`;

                row.querySelector('.keyword').addEventListener('click', () => {
                    searchInput.value = keyword;
                    searchInput.dispatchEvent(new Event('input'));
                });

                row.querySelector('.delete-btn').addEventListener('click', async () => {
                    try {
                        const del = await fetch(`/api/search-history/delete?keyword=${encodeURIComponent(keyword)}`, {
                            method: 'DELETE',
                            credentials: 'include',
                        });
                        if (del.ok) {
                            showSearchHistory(filterKeyword);
                        }
                    } catch (err) {
                        console.error('Error deleting search history item:', err);
                    }
                });

                searchSuggestions.appendChild(row);
            });
        }

        searchSuggestions.classList.add('active');
    } catch (error) {
        console.error('Error fetching search history:', error);
        searchSuggestions.innerHTML = `<div class="history-item">${error.message}</div>`;
        searchSuggestions.classList.add('active');
    }
}

// ===== Cart count =====
async function updateCartCount() {
    const el = document.getElementById('cart-count');
    if (!el) return;

    try {
        const response = await fetch('/api/cart', { credentials: 'include' });
        if (response.ok) {
            const data = await response.json();
            el.textContent = data.chiTietGioHang ? data.chiTietGioHang.length : 0;
        } else if (response.status === 401) {
            el.textContent = 0;
        }
    } catch (error) {
        console.error('Error updating cart count:', error);
    }
}
document.addEventListener('DOMContentLoaded', updateCartCount);