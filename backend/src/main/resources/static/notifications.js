(() => {
    'use strict';

    const notificationServiceOrigin = window.NOTIFICATION_SERVICE_ORIGIN
        || `${window.location.protocol}//${window.location.hostname}:8081`;
    const API_BASE = `${notificationServiceOrigin.replace(/\/$/, '')}/api/notifications`;
    const POLL_INTERVAL_MS = 15_000;
    const PAGE_SIZE = 8;

    class NotificationCenter {
        constructor() {
            this.token = sessionStorage.getItem('jwt_token');
            this.notifications = [];
            this.knownNotificationIds = new Set();
            this.initialized = false;
            this.loading = false;
            this.pollTimer = null;
            this.lastUpdatedAt = null;
        }

        mount() {
            if (!this.token || document.getElementById('notificationCenter')) {
                return false;
            }

            const profileDropdown = document.querySelector('.profile-toggle')?.closest('.dropdown');
            if (!profileDropdown) {
                return false;
            }

            const root = document.createElement('div');
            root.id = 'notificationCenter';
            root.className = 'dropdown notification-center me-2';
            root.innerHTML = `
                <button class="notification-bell" id="notificationBell" type="button"
                        data-bs-toggle="dropdown" data-bs-auto-close="outside"
                        aria-expanded="false" aria-label="Открыть уведомления">
                    <i class="bi bi-bell-fill" aria-hidden="true"></i>
                    <span class="notification-badge d-none" id="notificationBadge" aria-label="Непрочитанные уведомления">0</span>
                </button>
                <div class="dropdown-menu dropdown-menu-end notification-dropdown" aria-labelledby="notificationBell">
                    <div class="notification-dropdown-header">
                        <div>
                            <h2 class="notification-dropdown-title">Уведомления</h2>
                            <div class="notification-dropdown-subtitle" id="notificationSummary">Проверяем обновления…</div>
                        </div>
                        <button class="notification-mark-all" id="notificationMarkAll" type="button" disabled>
                            <i class="bi bi-check2-all me-1" aria-hidden="true"></i>Прочитать все
                        </button>
                    </div>
                    <div class="notification-list" id="notificationList" aria-live="polite">
                        <div class="notification-loading">
                            <span class="spinner-border spinner-border-sm text-primary" aria-hidden="true"></span>
                            <span class="ms-2">Загружаем уведомления…</span>
                        </div>
                    </div>
                    <div class="notification-dropdown-footer">
                        <span id="notificationLastUpdated">Ещё не обновлялось</span>
                        <button class="notification-refresh" id="notificationRefresh" type="button">
                            <i class="bi bi-arrow-clockwise me-1" aria-hidden="true"></i>Обновить
                        </button>
                    </div>
                </div>`;

            profileDropdown.insertAdjacentElement('beforebegin', root);

            this.root = root;
            this.bell = root.querySelector('#notificationBell');
            this.badge = root.querySelector('#notificationBadge');
            this.summary = root.querySelector('#notificationSummary');
            this.list = root.querySelector('#notificationList');
            this.markAllButton = root.querySelector('#notificationMarkAll');
            this.refreshButton = root.querySelector('#notificationRefresh');
            this.lastUpdated = root.querySelector('#notificationLastUpdated');

            this.ensureToastContainer();
            this.bindEvents();
            return true;
        }

        bindEvents() {
            this.refreshButton.addEventListener('click', () => this.refresh());
            this.markAllButton.addEventListener('click', () => this.markAllAsRead());
            this.root.addEventListener('show.bs.dropdown', () => this.refresh({silent: true}));

            document.addEventListener('visibilitychange', () => {
                if (!document.hidden) {
                    this.refresh({silent: true});
                }
            });

            window.addEventListener('beforeunload', () => this.stopPolling());
        }

        async start() {
            if (!this.mount()) {
                return;
            }

            await this.refresh({initial: true});
            this.startPolling();
            this.focusTaskFromQuery();
        }

        startPolling() {
            this.stopPolling();
            this.pollTimer = window.setInterval(() => {
                if (!document.hidden) {
                    this.refresh({silent: true});
                }
            }, POLL_INTERVAL_MS);
        }

        stopPolling() {
            if (this.pollTimer !== null) {
                window.clearInterval(this.pollTimer);
                this.pollTimer = null;
            }
        }

        async refresh({initial = false, silent = false} = {}) {
            if (this.loading) {
                return;
            }

            this.loading = true;
            this.refreshButton?.setAttribute('disabled', 'disabled');

            if (!silent && !this.initialized) {
                this.renderLoading();
            }

            try {
                const [page, unreadCount] = await Promise.all([
                    this.request(`${API_BASE}?page=0&size=${PAGE_SIZE}`),
                    this.request(`${API_BASE}/unread-count`)
                ]);

                const incoming = Array.isArray(page.content) ? page.content : [];
                const newNotifications = this.initialized
                    ? incoming.filter(item => !this.knownNotificationIds.has(item.id) && !item.read)
                    : [];

                this.notifications = incoming;
                incoming.forEach(item => this.knownNotificationIds.add(item.id));
                this.updateBadge(Number(unreadCount) || 0);
                this.renderList();

                this.lastUpdatedAt = new Date();
                this.renderLastUpdated();
                this.initialized = true;

                if (!initial && newNotifications.length > 0) {
                    this.showToast(newNotifications[0]);
                    this.animateBell();
                }
            } catch (error) {
                console.error('Не удалось загрузить уведомления:', error);
                if (!silent || !this.initialized) {
                    this.renderError();
                }
            } finally {
                this.loading = false;
                this.refreshButton?.removeAttribute('disabled');
            }
        }

        async request(url, options = {}) {
            const response = await fetch(url, {
                ...options,
                headers: {
                    'Authorization': `Bearer ${this.token}`,
                    ...(options.headers || {})
                }
            });

            if (response.status === 401 || response.status === 403) {
                this.stopPolling();
                if (typeof window.logout === 'function') {
                    window.logout();
                } else {
                    sessionStorage.removeItem('jwt_token');
                    window.location.href = '/login';
                }
                throw new Error('Сессия завершена');
            }

            if (!response.ok) {
                throw new Error(`Сервер вернул ${response.status}`);
            }

            if (response.status === 204) {
                return null;
            }

            return response.json();
        }

        renderLoading() {
            this.list.innerHTML = `
                <div class="notification-loading">
                    <span class="spinner-border spinner-border-sm text-primary" aria-hidden="true"></span>
                    <span class="ms-2">Загружаем уведомления…</span>
                </div>`;
        }

        renderError() {
            this.list.replaceChildren();
            const container = document.createElement('div');
            container.className = 'notification-error';
            container.innerHTML = `
                <div class="notification-error-icon"><i class="bi bi-wifi-off" aria-hidden="true"></i></div>
                <strong>Не удалось загрузить</strong>
                <span>Проверьте соединение и попробуйте ещё раз</span>`;
            this.list.appendChild(container);
            this.summary.textContent = 'Нет соединения';
        }

        renderList() {
            this.list.replaceChildren();

            if (this.notifications.length === 0) {
                const empty = document.createElement('div');
                empty.className = 'notification-empty';
                empty.innerHTML = `
                    <div class="notification-empty-icon"><i class="bi bi-bell-slash" aria-hidden="true"></i></div>
                    <strong>Пока всё спокойно</strong>
                    <span>Новые назначения появятся здесь</span>`;
                this.list.appendChild(empty);
                return;
            }

            const fragment = document.createDocumentFragment();
            this.notifications.forEach(notification => {
                fragment.appendChild(this.createNotificationItem(notification));
            });
            this.list.appendChild(fragment);
        }

        createNotificationItem(notification) {
            const item = document.createElement('button');
            item.type = 'button';
            item.className = `notification-item${notification.read ? '' : ' is-unread'}`;
            item.setAttribute('aria-label', `${notification.title}. ${notification.message}`);

            const icon = document.createElement('span');
            icon.className = 'notification-icon';
            const iconElement = document.createElement('i');
            iconElement.className = notification.notificationType === 'TASK_STATUS_CHANGED'
                ? 'bi bi-arrow-repeat'
                : 'bi bi-person-check-fill';
            iconElement.setAttribute('aria-hidden', 'true');
            icon.appendChild(iconElement);

            const content = document.createElement('span');
            content.className = 'notification-content';

            const title = document.createElement('span');
            title.className = 'notification-item-title';
            title.textContent = notification.title || 'Новое уведомление';
            if (!notification.read) {
                const dot = document.createElement('span');
                dot.className = 'notification-unread-dot';
                dot.setAttribute('aria-hidden', 'true');
                title.prepend(dot);
            }

            const message = document.createElement('span');
            message.className = 'notification-message';
            message.textContent = notification.message || '';

            const meta = document.createElement('span');
            meta.className = 'notification-meta';

            const time = document.createElement('span');
            time.textContent = this.formatDate(notification.createdAt);
            meta.appendChild(time);

            if (notification.taskId != null) {
                const task = document.createElement('span');
                task.className = 'notification-task-label';
                task.textContent = `Задача #${notification.taskId}`;
                meta.appendChild(task);
            }

            content.append(title, message, meta);

            const chevron = document.createElement('i');
            chevron.className = 'bi bi-chevron-right notification-chevron';
            chevron.setAttribute('aria-hidden', 'true');

            item.append(icon, content, chevron);
            item.addEventListener('click', () => this.openNotification(notification));
            return item;
        }

        updateBadge(unreadCount) {
            this.unreadCount = unreadCount;
            const hasUnread = unreadCount > 0;
            this.badge.textContent = unreadCount > 99 ? '99+' : String(unreadCount);
            this.badge.classList.toggle('d-none', !hasUnread);
            this.bell.classList.toggle('has-unread', hasUnread);
            this.bell.setAttribute(
                'aria-label',
                hasUnread ? `Уведомления: непрочитанных ${unreadCount}` : 'Уведомления: новых нет'
            );
            this.summary.textContent = hasUnread
                ? `Непрочитанных: ${unreadCount}`
                : 'Новых уведомлений нет';
            this.markAllButton.disabled = !hasUnread;
        }

        async openNotification(notification) {
            try {
                if (!notification.read) {
                    const updated = await this.request(`${API_BASE}/${notification.id}/read`, {
                        method: 'PATCH'
                    });
                    Object.assign(notification, updated);
                    this.updateBadge(Math.max(0, this.unreadCount - 1));
                    this.renderList();
                }

                if (notification.taskId != null) {
                    this.goToTask(notification.taskId);
                }
            } catch (error) {
                console.error('Не удалось отметить уведомление прочитанным:', error);
                this.showErrorToast('Не удалось обновить уведомление');
            }
        }

        async markAllAsRead() {
            if (!this.unreadCount) {
                return;
            }

            this.markAllButton.disabled = true;
            try {
                await this.request(`${API_BASE}/read-all`, {method: 'PATCH'});
                const readAt = new Date().toISOString();
                this.notifications.forEach(notification => {
                    if (!notification.read) {
                        notification.read = true;
                        notification.readAt = readAt;
                    }
                });
                this.updateBadge(0);
                this.renderList();
            } catch (error) {
                console.error('Не удалось прочитать все уведомления:', error);
                this.showErrorToast('Не удалось отметить уведомления прочитанными');
                this.markAllButton.disabled = false;
            }
        }

        goToTask(taskId) {
            const onTasksPage = window.location.pathname === '/tasks'
                || window.location.pathname.endsWith('/tasks.html');

            if (!onTasksPage) {
                window.location.href = `/tasks?focusTask=${encodeURIComponent(taskId)}`;
                return;
            }

            const row = document.querySelector(`tr[data-task-id="${String(taskId)}"]`);
            if (!row) {
                window.location.href = `/tasks?focusTask=${encodeURIComponent(taskId)}`;
                return;
            }

            bootstrap.Dropdown.getOrCreateInstance(this.bell).hide();
            this.focusTaskRow(taskId);
        }

        focusTaskFromQuery() {
            const taskId = new URLSearchParams(window.location.search).get('focusTask');
            if (taskId) {
                this.focusTaskRow(taskId, 24);
            }
        }

        focusTaskRow(taskId, attemptsLeft = 1) {
            const row = document.querySelector(`tr[data-task-id="${String(taskId)}"]`);
            if (row) {
                row.scrollIntoView({behavior: 'smooth', block: 'center'});
                row.classList.remove('task-notification-highlight');
                window.requestAnimationFrame(() => row.classList.add('task-notification-highlight'));
                return;
            }

            if (attemptsLeft > 1) {
                window.setTimeout(() => this.focusTaskRow(taskId, attemptsLeft - 1), 250);
            }
        }

        formatDate(value) {
            const date = new Date(value);
            if (Number.isNaN(date.getTime())) {
                return '';
            }

            const diffSeconds = Math.round((date.getTime() - Date.now()) / 1000);
            const relative = new Intl.RelativeTimeFormat('ru', {numeric: 'auto'});

            if (Math.abs(diffSeconds) < 60) {
                return relative.format(diffSeconds, 'second');
            }

            const diffMinutes = Math.round(diffSeconds / 60);
            if (Math.abs(diffMinutes) < 60) {
                return relative.format(diffMinutes, 'minute');
            }

            const diffHours = Math.round(diffMinutes / 60);
            if (Math.abs(diffHours) < 24) {
                return relative.format(diffHours, 'hour');
            }

            const diffDays = Math.round(diffHours / 24);
            if (Math.abs(diffDays) < 7) {
                return relative.format(diffDays, 'day');
            }

            return date.toLocaleString('ru-RU', {
                day: '2-digit',
                month: 'short',
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        renderLastUpdated() {
            if (!this.lastUpdatedAt) {
                return;
            }

            this.lastUpdated.textContent = `Обновлено ${this.lastUpdatedAt.toLocaleTimeString('ru-RU', {
                hour: '2-digit',
                minute: '2-digit'
            })}`;
        }

        animateBell() {
            this.bell.classList.remove('has-unread');
            window.requestAnimationFrame(() => this.bell.classList.add('has-unread'));
        }

        ensureToastContainer() {
            let container = document.getElementById('notificationToastContainer');
            if (!container) {
                container = document.createElement('div');
                container.id = 'notificationToastContainer';
                container.className = 'toast-container position-fixed top-0 end-0 p-3';
                container.style.zIndex = '1090';
                document.body.appendChild(container);
            }
            this.toastContainer = container;
        }

        showToast(notification) {
            const toast = document.createElement('div');
            toast.className = 'toast notification-toast';
            toast.setAttribute('role', 'alert');
            toast.setAttribute('aria-live', 'assertive');
            toast.setAttribute('aria-atomic', 'true');

            const header = document.createElement('div');
            header.className = 'toast-header';
            header.innerHTML = `
                <span class="notification-icon me-2" style="width:30px;height:30px;border-radius:9px">
                    <i class="bi bi-person-check-fill" aria-hidden="true"></i>
                </span>
                <strong class="me-auto"></strong>
                <small class="text-body-secondary">сейчас</small>
                <button type="button" class="btn-close ms-2 mb-1" data-bs-dismiss="toast" aria-label="Закрыть"></button>`;
            header.querySelector('strong').textContent = notification.title || 'Новое уведомление';

            const body = document.createElement('button');
            body.type = 'button';
            body.className = 'toast-body border-0 bg-white text-start w-100';
            body.textContent = notification.message || '';
            body.addEventListener('click', () => {
                bootstrap.Toast.getOrCreateInstance(toast).hide();
                this.openNotification(notification);
            });

            toast.append(header, body);
            this.toastContainer.appendChild(toast);

            const instance = new bootstrap.Toast(toast, {delay: 7000});
            toast.addEventListener('hidden.bs.toast', () => toast.remove(), {once: true});
            instance.show();
        }

        showErrorToast(message) {
            const toast = document.createElement('div');
            toast.className = 'toast notification-toast border-danger';
            toast.setAttribute('role', 'alert');
            toast.innerHTML = `
                <div class="d-flex align-items-center">
                    <div class="toast-body text-danger"></div>
                    <button type="button" class="btn-close me-2" data-bs-dismiss="toast" aria-label="Закрыть"></button>
                </div>`;
            toast.querySelector('.toast-body').textContent = message;
            this.toastContainer.appendChild(toast);
            toast.addEventListener('hidden.bs.toast', () => toast.remove(), {once: true});
            new bootstrap.Toast(toast, {delay: 4500}).show();
        }
    }

    const notificationCenter = new NotificationCenter();
    window.notificationCenter = notificationCenter;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => notificationCenter.start());
    } else {
        notificationCenter.start();
    }
})();
