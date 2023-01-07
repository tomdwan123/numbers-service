CREATE OR REPLACE FUNCTION public.castservicetypes(txt text)
 RETURNS service_type[]
 LANGUAGE plpgsql
AS $function$
	begin
		return cast(string_to_array(txt, ',') as SERVICE_TYPE[]);
	END;
$function$
;

CREATE OR REPLACE FUNCTION public.array_sort(anyarray)
 RETURNS anyarray
 LANGUAGE sql
AS $function$
select array_agg(x order by x) FROM unnest($1) x;
$function$
;
