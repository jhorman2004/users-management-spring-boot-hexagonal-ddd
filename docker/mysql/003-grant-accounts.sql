-- Solo para el MySQL de compose.yaml: el usuario de la aplicación (users_app) también
-- trabaja con la base de cuentas. En TiDB cada servicio usa sus propias credenciales.
GRANT ALL PRIVILEGES ON crud_cuentas.* TO 'users_app'@'%';
FLUSH PRIVILEGES;
