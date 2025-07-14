DROP table public.vw_appuser_role;

create view vw_appuser_role as
select distinct row_number() OVER () AS id, f.username, f.email, f6.role_name as role
from
	fw_appuser f,
	fw_appuser_resp f2,
	fw_responsibilities f3,
	fw_responsibilities_menu f4,
	fw_menus f5,
	fw_pages f6
where
	f.id = f2.appuser_id and
	f2.responsibility_id = f3.id and
	f3.id = f4.responsibility_id and
	f4.menu_id = f5.id and
	f5.page_id = f6.id
order by f6.role_name asc;