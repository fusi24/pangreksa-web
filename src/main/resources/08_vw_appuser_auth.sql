DROP table public.vw_appuser_auth;

CREATE OR REPLACE VIEW public.vw_appuser_auth
AS SELECT row_number() OVER () AS id,
    f.username,
    f.email,
    f.nickname,
    f3.label AS responsibility,
    f5.label,
    f6.role_name AS role,
    f6.description,
    f6.page_url AS url,
    f5.sort_order,
    f6.page_icon,
    f3.is_active
   FROM fw_appuser f,
    fw_appuser_resp f2,
    fw_responsibilities f3,
    fw_responsibilities_menu f4,
    fw_menus f5,
    fw_pages f6
  WHERE f.id = f2.appuser_id AND f2.responsibility_id = f3.id AND f3.id = f4.responsibility_id AND f4.menu_id = f5.id AND f5.page_id = f6.id
  ORDER BY f5.sort_order;