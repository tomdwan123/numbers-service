CREATE OR REPLACE FUNCTION castServiceType(s TEXT) RETURNS SERVICE_TYPE AS $$
        BEGIN
                RETURN CAST(s AS SERVICE_TYPE);
        END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION regex(s TEXT, regex TEXT) RETURNS BOOLEAN AS $$
        BEGIN
                RETURN s ~ regex;
        END;
$$ LANGUAGE plpgsql;