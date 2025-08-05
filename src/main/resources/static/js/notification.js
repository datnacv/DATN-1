document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM loaded, initializing notification system...');

    if (typeof bootstrap === 'undefined') {
        console.error('Bootstrap JavaScript not loaded. Ensure Bootstrap 5.1.3 is included.');
        alert('Lỗi hệ thống: Không thể tải thư viện giao diện. Vui lòng thử lại sau.');
        return;
    }

    let currentPage = 0;
    let totalPages = 1;
    let unreadCount = 0;
    let isDropdownOpen = false;
    let cachedNotifications = null;
    let lastFetchTime = 0;
    const CACHE_DURATION = 30000; // Cache trong 30 giây

    // Hàm debounce để ngăn gọi API nhiều lần
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Hàm hiển thị toast
    function showToast(message, type = 'success') {
        const toastContainer = document.createElement('div');
        toastContainer.className = `position-fixed top-0 end-0 p-3`; // Thay đổi từ bottom-0 thành top-0
        toastContainer.style.zIndex = '1050';
        toastContainer.innerHTML = `
        <div class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;
        document.body.appendChild(toastContainer);
        const toast = new bootstrap.Toast(toastContainer.querySelector('.toast'));
        toast.show();
        setTimeout(() => toastContainer.remove(), 3000);
    }

    // Xử lý chuông thông báo
    const trigger = document.getElementById('notificationDropdown');
    if (trigger) {
        const dropdown = trigger.closest('.dropdown');
        if (dropdown) {
            trigger.addEventListener('click', debounce(function () {
                console.log('Notification bell clicked, triggering loadNotifications...');
                isDropdownOpen = true;
                loadNotifications();
            }, 300));
            const dropdownMenu = document.getElementById('notificationContainer');
            dropdownMenu.addEventListener('hidden.bs.dropdown', function () {
                isDropdownOpen = false;
            });
        } else {
            console.warn('Dropdown container not found for notificationDropdown');
        }
    } else {
        console.warn('Notification dropdown trigger not found');
    }

    // Tải thông báo cho bảng nếu có
    const tableBody = document.getElementById('notificationTableBody');
    if (tableBody) {
        loadNotificationsForTable();
    }

    // Xử lý liên kết "Xem tất cả"
    const viewAllLinks = document.querySelectorAll('a[data-bs-target="#notificationModal"]');
    viewAllLinks.forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            console.log('View all notifications link clicked');
            const modal = document.getElementById('notificationModal');
            if (!modal) {
                console.error('Modal element not found for ID: notificationModal');
                alert('Lỗi hệ thống: Không tìm thấy cửa sổ thông báo.');
                return;
            }
            try {
                console.log('Attempting to initialize and show modal');
                const modalInstance = new bootstrap.Modal(modal);
                modalInstance.show();
                console.log('Modal opened successfully');
                modal.addEventListener('shown.bs.modal', function () {
                    console.log('Modal is shown, loading notifications...');
                    currentPage = 0;
                    loadNotificationsForModal();
                }, { once: true });
            } catch (e) {
                console.error('Error showing modal:', e);
                alert('Lỗi hệ thống: Không thể hiển thị cửa sổ thông báo. Vui lòng thử lại sau.');
            }
        });
    });

    function loadNotifications() {
        const loadingState = document.getElementById('loadingState');
        const emptyState = document.getElementById('emptyState');
        const notificationContainer = document.getElementById('notificationContainer');
        const anchor = document.getElementById('notificationInsertAnchor');

        if (!notificationContainer || !anchor) {
            console.error('Notification container or anchor not found');
            return;
        }

        // Xóa các thông báo cũ
        const oldItems = notificationContainer.querySelectorAll('.notification-item:not(#loadingState):not(#emptyState)');
        oldItems.forEach(item => item.remove());

        const now = Date.now();
        if (cachedNotifications && (now - lastFetchTime < CACHE_DURATION)) {
            console.log('Sử dụng dữ liệu từ cache');
            if (loadingState) loadingState.classList.add('d-none');
            if (cachedNotifications.length > 0) {
                renderNotifications(cachedNotifications);
                updateUnreadCount(unreadCount);
            } else {
                if (emptyState) emptyState.classList.remove('d-none');
            }
            checkEmptyState();
            return;
        }

        if (loadingState) loadingState.classList.remove('d-none');
        if (emptyState) emptyState.classList.add('d-none');

        fetch('/acvstore/thong-bao/load?unread=true&limit=5&_=' + now)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Dữ liệu thông báo:', data);
                if (loadingState) loadingState.classList.add('d-none');

                const unreadNotifications = Array.isArray(data.notifications)
                    ? data.notifications.filter(n => !n.daXem).slice(0, 5)
                    : [];

                cachedNotifications = unreadNotifications;
                lastFetchTime = now;
                unreadCount = data.unreadCount || 0;

                if (unreadNotifications.length > 0) {
                    renderNotifications(unreadNotifications);
                    updateUnreadCount(unreadCount);
                } else {
                    if (emptyState) emptyState.classList.remove('d-none');
                }

                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi tải thông báo:', error);
                if (loadingState) loadingState.classList.add('d-none');
                const errorItem = document.createElement('div');
                errorItem.className = 'alert alert-danger m-3';
                errorItem.innerHTML = 'Không thể tải thông báo. Vui lòng thử lại sau.';
                anchor.before(errorItem);
            });
    }



    function loadNotificationsForTable() {
        const loadingState = document.getElementById('tableLoadingState');
        const emptyState = document.getElementById('tableEmptyState');
        const tableBody = document.getElementById('notificationTableBody');

        if (!tableBody) {
            console.error('Table body not found');
            return;
        }

        if (loadingState) loadingState.classList.remove('d-none');
        if (emptyState) emptyState.classList.add('d-none');

        const existingNotifications = tableBody.querySelectorAll('tr:not(#tableLoadingState):not(#tableEmptyState)') || [];
        existingNotifications.forEach(item => item.remove());

        fetch('/acvstore/thong-bao/load?_=' + new Date().getTime())
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Số thông báo nhận được (table):', data.notifications ? data.notifications.length : 0);
                if (loadingState) loadingState.classList.add('d-none');
                if (data.notifications && Array.isArray(data.notifications) && data.notifications.length > 0) {
                    renderNotificationsForTable(data.notifications.slice(0, 5), false);
                    unreadCount = data.unreadCount || 0;
                    updateUnreadCount(unreadCount);
                } else {
                    if (emptyState) emptyState.classList.remove('d-none');
                }
                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi tải thông báo cho bảng:', error);
                if (loadingState) loadingState.classList.add('d-none');
                const errorRow = document.createElement('tr');
                errorRow.innerHTML = `
                    <td colspan="5" class="text-center">
                        <div class="alert alert-danger m-3">Không thể tải thông báo. Vui lòng thử lại sau.</div>
                    </td>
                `;
                tableBody.appendChild(errorRow);
            });
    }

    function loadNotificationsForModal(page = currentPage, size = 5) {
        const loadingState = document.getElementById('modalTableLoadingState');
        const emptyState = document.getElementById('modalTableEmptyState');
        const tableBody = document.getElementById('modalNotificationTableBody');

        if (!tableBody) {
            console.error('Modal table body not found');
            return;
        }

        if (loadingState) loadingState.classList.remove('d-none');
        if (emptyState) emptyState.classList.add('d-none');

        const existingNotifications = tableBody.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)') || [];
        existingNotifications.forEach(item => item.remove());

        console.log(`Loading notifications for modal with page=${page}, size=${size}`);
        fetch(`/acvstore/thong-bao/xem?page=${page}&size=${size}&_=${new Date().getTime()}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Số thông báo nhận được (modal):', data.notifications ? data.notifications.length : 0);
                if (loadingState) loadingState.classList.add('d-none');
                if (data.notifications && Array.isArray(data.notifications) && data.notifications.length > 0) {
                    renderNotificationsForTable(data.notifications, true);
                    unreadCount = data.unreadCount || 0;
                    updateUnreadCount(unreadCount);
                    totalPages = Math.max(1, Math.ceil((data.totalCount || data.notifications.length) / size));
                    updatePagination(page);
                } else {
                    if (emptyState) emptyState.classList.remove('d-none');
                    totalPages = 1;
                    updatePagination(page);
                }
                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi tải thông báo cho modal:', error);
                if (loadingState) loadingState.classList.add('d-none');
                const errorRow = document.createElement('tr');
                errorRow.innerHTML = `
                    <td colspan="5" class="text-center">
                        <div class="alert alert-danger m-3">Không thể tải thông báo. Vui lòng thử lại sau.</div>
                    </td>
                `;
                tableBody.appendChild(errorRow);
            });
    }

    function renderNotifications(notifications) {
        const anchor = document.getElementById('notificationInsertAnchor');
        const emptyState = document.getElementById('emptyState');
        const notificationContainer = document.getElementById('notificationContainer');

        if (!anchor || !notificationContainer) {
            console.error('Notification insert anchor or container not found');
            return;
        }

        // 🔥 FIX: Xóa tất cả các thông báo cũ để tránh trùng
        const oldItems = notificationContainer.querySelectorAll('.notification-item:not(#loadingState):not(#emptyState)');
        oldItems.forEach(item => item.remove());

        // Không slice ở đây nữa – dữ liệu đã được giới hạn từ loadNotifications()
        if (!notifications || notifications.length === 0) {
            if (emptyState) emptyState.classList.remove('d-none');
            return;
        }

        notifications.forEach((notification, index) => {
            const timeAgo = formatTimeAgo(notification.thoiGian);
            const icon = '<i class="fas fa-info-circle text-primary"></i>';
            const tieuDe = notification.tieuDe || 'Không có tiêu đề';
            const noiDung = notification.noiDung || 'Không có nội dung';

            const item = document.createElement('li');
            item.className = `notification-item px-3 py-2 ${notification.daXem ? '' : 'notification-unread'}`;
            item.style.cursor = 'pointer';
            item.dataset.notificationId = notification.idChiTietThongBao;
            item.onclick = () => showConfirmModal(notification.idChiTietThongBao, item, tieuDe);

            item.innerHTML = `
            <div class="d-flex align-items-start">
                <div class="me-2">${icon}</div>
                <div class="flex-grow-1">
                    <div class="fw-semibold mb-1">${escapeHtml(tieuDe)}</div>
                    <div class="text-muted notification-content">${escapeHtml(noiDung)}</div>
                    <div class="text-end notification-time mt-1">${timeAgo}</div>
                </div>
                ${notification.daXem ? '' : '<span class="badge bg-primary ms-2">Mới</span>'}
            </div>
        `;
            anchor.before(item);
        });
    }



    function renderNotificationsForTable(notifications, isModal = false) {
        const tableBody = isModal ? document.getElementById('modalNotificationTableBody') : document.getElementById('notificationTableBody');
        const emptyState = isModal ? document.getElementById('modalTableEmptyState') : document.getElementById('tableEmptyState');

        if (!tableBody) {
            console.error('Table body not found');
            return;
        }

        tableBody.classList.add('opacity-50');

        if (!notifications || !Array.isArray(notifications) || notifications.length === 0) {
            if (emptyState) emptyState.classList.remove('d-none');
            tableBody.classList.remove('opacity-50');
            return;
        }

        const existingRows = tableBody.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)');
        existingRows.forEach(item => item.remove());

        const baseIndex = currentPage * 5;

        notifications.forEach((notification, index) => {
            const timeAgo = formatTimeAgo(notification.thoiGian);
            const tieuDe = notification.tieuDe || 'Không có tiêu đề';
            const noiDung = notification.noiDung || 'Không có nội dung';

            const row = document.createElement('tr');
            row.className = notification.daXem ? '' : 'notification-unread';
            row.style.cursor = 'pointer';
            row.dataset.notificationId = notification.idChiTietThongBao;
            row.onclick = () => showConfirmModal(notification.idChiTietThongBao, row, tieuDe);

            row.innerHTML = `
                <td>${baseIndex + index + 1}</td>
                <td>${escapeHtml(tieuDe)}</td>
                <td>${escapeHtml(noiDung)}</td>
                <td>${timeAgo}</td>
                <td>${notification.daXem ? 'Đã đọc' : '<span class="badge bg-primary">Mới</span>'}</td>
            `;

            tableBody.appendChild(row);
        });

        setTimeout(() => {
            tableBody.classList.remove('opacity-50');
        }, 100);
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatTimeAgo(thoiGian) {
        if (!thoiGian) {
            console.warn('thoiGian is null or undefined');
            return 'Không rõ thời gian';
        }
        try {
            let dateObj;
            if (typeof thoiGian === 'string') {
                if (thoiGian.includes('/')) {
                    const parts = thoiGian.split(/[/ :]/);
                    dateObj = new Date(`${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}T${parts[3].padStart(2, '0')}:${parts[4].padStart(2, '0')}:00`);
                } else {
                    dateObj = new Date(thoiGian);
                }
            } else if (typeof thoiGian === 'number') {
                dateObj = new Date(thoiGian);
            } else {
                console.warn('Invalid thoiGian format:', thoiGian);
                return 'Không rõ thời gian';
            }

            if (!dateObj || isNaN(dateObj.getTime())) {
                console.warn('Invalid date object for thoiGian:', thoiGian);
                return 'Không rõ thời gian';
            }

            const now = new Date();
            const seconds = Math.floor((now - dateObj) / 1000);
            if (seconds < 60) return 'Vừa xong';
            const minutes = Math.floor(seconds / 60);
            if (minutes < 60) return `${minutes} phút trước`;
            const hours = Math.floor(minutes / 60);
            if (hours < 24) return `${hours} giờ trước`;
            const days = Math.floor(hours / 24);
            if (days < 7) return `${days} ngày trước`;
            return dateObj.toLocaleDateString('vi-VN', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            console.error('Lỗi định dạng thời gian:', e, 'thoiGian:', thoiGian);
            return 'Không rõ thời gian';
        }
    }

    function showConfirmModal(notificationId, element, tieuDe = 'Thông báo') {
        console.log('Hiển thị modal xác nhận cho idChiTietThongBao:', notificationId);

        const modal = document.getElementById('confirmReadModal');
        if (!modal) {
            console.error('Confirm modal not found');
            alert('Lỗi hệ thống: Không tìm thấy cửa sổ xác nhận.');
            return;
        }

        const modalBody = modal.querySelector('.modal-body');
        modalBody.textContent = `Bạn có chắc chắn muốn đánh dấu thông báo "${escapeHtml(tieuDe)}" là đã đọc?`;

        const modalInstance = new bootstrap.Modal(modal);
        modalInstance.show();

        // Lưu trạng thái dropdown đang mở để tránh reload lại
        const wasDropdownOpen = isDropdownOpen;

        // Gắn lại sự kiện cho nút Xác nhận (để tránh duplicate listener)
        const confirmButton = document.getElementById('confirmReadButton');
        const newConfirmButton = confirmButton.cloneNode(true);
        confirmButton.parentNode.replaceChild(newConfirmButton, confirmButton);

        newConfirmButton.addEventListener('click', () => {
            console.log('Xác nhận đánh dấu đã đọc:', notificationId);
            markAsRead(notificationId, element);
            modalInstance.hide();
        });

        modal.addEventListener('hidden.bs.modal', function () {
            console.log('Modal xác nhận đóng, dropdown đang mở trước đó?', wasDropdownOpen);

            // Nếu dropdown đang mở, nhưng user chỉ đóng modal thì không reload dropdown
            if (wasDropdownOpen) {
                isDropdownOpen = false;
                // Nếu dropdown bị mở lại do lỗi, thì tắt dropdown
                const dropdownEl = document.getElementById('notificationDropdown');
                if (dropdownEl) {
                    const dropdownInstance = bootstrap.Dropdown.getInstance(dropdownEl);
                    if (dropdownInstance) {
                        dropdownInstance.hide();
                    }
                }
            }
        }, { once: true });
    }


    function markAsRead(notificationId, element) {
        fetch('/acvstore/thong-bao/danh-dau-da-xem', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `idChiTietThongBao=${encodeURIComponent(notificationId)}`
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Lỗi server: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Dữ liệu từ server:', data);
                element.classList.remove('notification-unread');
                const badge = element.querySelector('.badge');
                if (badge) badge.remove();

                if (element.tagName === 'TR') {
                    const statusCell = element.querySelector('td:last-child');
                    if (statusCell) statusCell.textContent = 'Đã đọc';
                }

                unreadCount = data.unreadCount || Math.max(0, unreadCount - 1);
                updateUnreadCount(unreadCount);
                updateNotificationInLists(notificationId);

                // Xóa thông báo khỏi dropdown ngay lập tức
                if (element.classList.contains('notification-item')) {
                    element.remove();
                    checkEmptyState();
                }

                showToast('Thông báo đã được đánh dấu là đã đọc!');
                if (isDropdownOpen) {
                    loadNotifications();
                }
            })
            .catch(error => {
                console.error('Lỗi khi đánh dấu đã đọc:', error);
                showToast('Không thể đánh dấu thông báo đã đọc.', 'danger');
            });
    }

    function updateNotificationInLists(notificationId) {
        const idString = notificationId.toString();
        console.log('Cập nhật giao diện cho idChiTietThongBao:', idString);
        const dropdownItems = document.querySelectorAll('#notificationContainer .notification-item');
        dropdownItems.forEach(item => {
            if (item.dataset.notificationId === idString) {
                console.log('Cập nhật dropdown item:', item.dataset.notificationId);
                item.remove(); // Xóa khỏi dropdown
            }
        });
        const tableRows = document.querySelectorAll('#notificationTableBody tr');
        tableRows.forEach(row => {
            if (row.dataset.notificationId === idString) {
                console.log('Cập nhật table row:', row.dataset.notificationId);
                row.classList.remove('notification-unread');
                row.style.cursor = 'default';
                const statusCell = row.querySelector('td:last-child');
                if (statusCell) statusCell.textContent = 'Đã đọc';
            }
        });
        const modalRows = document.querySelectorAll('#modalNotificationTableBody tr');
        modalRows.forEach(row => {
            if (row.dataset.notificationId === idString) {
                console.log('Cập nhật modal row:', row.dataset.notificationId);
                row.classList.remove('notification-unread');
                row.style.cursor = 'default';
                const statusCell = row.querySelector('td:last-child');
                if (statusCell) statusCell.textContent = 'Đã đọc';
            }
        });
    }

    function markAllAsRead() {
        fetch('/acvstore/thong-bao/danh-dau-tat-ca', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}, Nội dung: ${text}`);
                    });
                }
                return response.json();
            })
            .then(data => {
                console.log('Đánh dấu tất cả đã đọc - Response:', data);
                const unreadItems = document.querySelectorAll('.notification-unread');
                unreadItems.forEach(item => {
                    item.classList.remove('notification-unread');
                    item.style.cursor = 'default';
                    const indicator = item.querySelector('.badge');
                    if (indicator) indicator.remove();
                    if (item.tagName === 'TR') {
                        const statusCell = item.querySelector('td:last-child');
                        if (statusCell) statusCell.textContent = 'Đã đọc';
                    }
                    if (item.classList.contains('notification-item')) {
                        item.remove();
                    }
                });
                unreadCount = data.unreadCount || 0;
                updateUnreadCount(unreadCount);
                const markAllButtons = document.querySelectorAll('[onclick="markAllAsRead()"]');
                markAllButtons.forEach(btn => btn.style.display = 'none');
                showToast('Đã đánh dấu tất cả thông báo là đã đọc!');
                if (document.getElementById('notificationContainer').classList.contains('show')) {
                    loadNotifications();
                }
                if (document.getElementById('notificationTableBody')) {
                    loadNotificationsForTable();
                }
                if (document.getElementById('modalNotificationTableBody')) {
                    loadNotificationsForModal();
                }
                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi đánh dấu tất cả thông báo đã đọc:', error);
                showToast('Không thể đánh dấu tất cả thông báo đã đọc.', 'danger');
            });
    }

    function updateUnreadCount(count) {
        unreadCount = count;
        const badge = document.querySelector('.notification-badge span');
        const badgeContainer = document.querySelector('.notification-badge');
        if (count > 0) {
            if (badge) badge.textContent = count;
            if (badgeContainer) badgeContainer.style.display = 'inline';
        } else {
            if (badgeContainer) badgeContainer.style.display = 'none';
        }
    }

    function checkEmptyState() {
        const modalTableBody = document.getElementById('modalNotificationTableBody');
        const tableBody = document.getElementById('notificationTableBody');
        const notificationContainer = document.getElementById('notificationContainer');

        if (modalTableBody) {
            const items = modalTableBody.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)');
            const emptyState = document.getElementById('modalTableEmptyState');
            if (items.length === 0 && emptyState) {
                emptyState.classList.remove('d-none');
            }
        }

        if (tableBody) {
            const items = tableBody.querySelectorAll('tr:not(#tableLoadingState):not(#tableEmptyState)');
            const emptyState = document.getElementById('tableEmptyState');
            if (items.length === 0 && emptyState) {
                emptyState.classList.remove('d-none');
            }
        }

        if (notificationContainer) {
            const items = notificationContainer.querySelectorAll('.notification-item:not(#loadingState):not(#emptyState)');
            const emptyState = document.getElementById('emptyState');
            if (items.length === 0 && emptyState) {
                emptyState.classList.remove('d-none');
            }
        }
    }

    function updatePagination(page) {
        const prevPage = document.getElementById('prevPage');
        const nextPage = document.getElementById('nextPage');
        const currentPageEl = document.getElementById('currentPage');

        if (currentPageEl) {
            currentPageEl.querySelector('.page-link').textContent = page + 1;
        }

        if (prevPage) {
            prevPage.classList.toggle('disabled', page === 0);
        }

        if (nextPage) {
            nextPage.classList.toggle('disabled', page >= totalPages - 1);
        }
    }

    window.changePage = function (delta) {
        const newPage = currentPage + delta;
        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            loadNotificationsForModal(currentPage);
        }
    };
});