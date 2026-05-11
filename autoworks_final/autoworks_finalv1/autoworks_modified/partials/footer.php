<?php // partials/footer.php ?>
</div><!-- .app-body -->
<script>
// ── Dropdown & Notification toggles ──────────────────────────────────────
(function () {
    const notifBtn   = document.getElementById('notifBtn');
    const notifPanel = document.getElementById('notifPanel');
    const userChip   = document.getElementById('userChip');
    const userMenu   = document.getElementById('userMenu');

    function closeAll () {
        notifPanel?.classList.remove('open');
        userMenu?.classList.remove('open');
    }

    notifBtn?.addEventListener('click', e => {
        e.stopPropagation();
        const open = notifPanel.classList.toggle('open');
        if (open) userMenu.classList.remove('open');
    });

    userChip?.addEventListener('click', e => {
        e.stopPropagation();
        const open = userMenu.classList.toggle('open');
        if (open) notifPanel.classList.remove('open');
    });

    document.addEventListener('click', closeAll);

    // ── Modal helpers ───────────────────────────────────────────────────
    document.querySelectorAll('[data-modal-open]').forEach(btn => {
        btn.addEventListener('click', () => {
            const m = document.getElementById(btn.dataset.modalOpen);
            m?.classList.add('open');
        });
    });

    document.querySelectorAll('[data-modal-close], .modal-overlay').forEach(el => {
        el.addEventListener('click', function (e) {
            if (e.target === this || this.hasAttribute('data-modal-close')) {
                this.closest('.modal-overlay')?.classList.remove('open');
                document.querySelectorAll('.modal-overlay').forEach(o => o.classList.remove('open'));
            }
        });
    });

    document.querySelectorAll('.modal').forEach(m => {
        m.addEventListener('click', e => e.stopPropagation());
    });

    // ── Tab switcher ────────────────────────────────────────────────────
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const group = btn.closest('[data-tab-group]') || document;
            group.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const target = btn.dataset.tab;
            document.querySelectorAll('.tab-pane').forEach(p => {
                p.style.display = p.id === target ? '' : 'none';
            });
        });
    });

    // initialise first tab
    document.querySelectorAll('[data-tab-group]').forEach(g => {
        const panes = g.querySelectorAll('.tab-pane');
        panes.forEach((p, i) => { p.style.display = i === 0 ? '' : 'none'; });
    });

    // ── Auto-dismiss flash messages ─────────────────────────────────────
    document.querySelectorAll('.alert[data-auto-close]').forEach(a => {
        setTimeout(() => {
            a.style.transition = 'opacity .5s';
            a.style.opacity = 0;
            setTimeout(() => a.remove(), 500);
        }, 4000);
    });
})();
</script>
</body>
</html>
