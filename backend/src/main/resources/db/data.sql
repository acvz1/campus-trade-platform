INSERT INTO category (name, sort_order, icon) VALUES
    ('教材', 10, 'book'),
    ('数码', 20, 'device'),
    ('宿舍用品', 30, 'home'),
    ('运动器材', 40, 'sport'),
    ('其他', 99, 'more')
ON CONFLICT DO NOTHING;
