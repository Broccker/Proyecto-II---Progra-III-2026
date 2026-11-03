USE proyectoII_reservas;

SET NAMES utf8mb4;

START TRANSACTION;


INSERT INTO usuarios (id, clave_hash, rol, nombre, telefono) VALUES
    ('111', '$2a$10$FHA97XNOke7VLgRbprpmPOmkISAfL4NHEjfm2TGJwBWBmxO3Puolu',
     'ADMINISTRADOR', NULL, NULL),
    ('222', '$2a$10$KQxPviBw5.PYi9QW1KGeCeicPTcaUGIt7XwVaq8ovtdrT/MuhecOK',
     'FUNCIONARIO', 'Ana Pérez', '88881111'),
    ('333', '$2a$10$imos4BGWrv7WlQEAM4w2OuQft3dFXxn4pZI9uu1VMNtxdnnBbpvZq',
     'FUNCIONARIO', 'Luis Rojas', '88882222'),
    ('444', '$2a$10$Dx4d7UBXVfskxhcG56fuAuEtuGoLy0Dr62Z0S7QfBoIHmNaY58eW.',
     'FUNCIONARIO', 'Pedro Pérez', '88884444');


INSERT INTO categorias (id, descripcion) VALUES
    ('CAT-000001', 'Proyectores'),
    ('CAT-000002', 'Salas de reuniones'),
    ('CAT-000003', 'Computadoras'),
    ('CAT-000004', 'Herramientas');


INSERT INTO recursos (id, categoria_id, descripcion) VALUES
    ('1001', 'CAT-000001', 'Proyector Epson EB-X51'),
    ('1002', 'CAT-000001', 'Proyector ViewSonic PX701'),
    ('2001', 'CAT-000002', 'Sala A (12 personas)'),
    ('2002', 'CAT-000002', 'Sala B (20 personas)'),
    ('3001', 'CAT-000003', 'Laptop Dell Latitude 5420'),
    ('3002', 'CAT-000003', 'Laptop Lenovo ThinkPad E14'),
    ('4001', 'CAT-000004', 'Kit de Precisión Xiaomi'),
    ('4002', 'CAT-000004', 'Kit de Precisión Imexx');


INSERT INTO reservas (id, funcionario_id, actividad, fecha, hora_inicio, hora_fin, estado) VALUES
    ('RES-000001', '222', 'Presentaciones (Hardware)',
     DATE_SUB(CURDATE(), INTERVAL 13 DAY), '08:00', '10:00', 'ACTIVA'),
    ('RES-000002', '222', 'Reunión de Directores',
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), '12:00', '13:00', 'ACTIVA'),
    ('RES-000003', '333', 'Expo Laboral',
     DATE_SUB(CURDATE(), INTERVAL 7 DAY), '14:00', '16:00', 'ACTIVA'),
    ('RES-000004', '444', 'Sesión de Estudiantes',
     DATE_SUB(CURDATE(), INTERVAL 3 DAY), '12:00', '17:00', 'ACTIVA'),
    ('RES-000005', '222', 'Capacitación Docente',
     DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:00', '11:00', 'ACTIVA'),
    ('RES-000006', '333', 'Mantenimiento de Equipos',
     DATE_ADD(CURDATE(), INTERVAL 5 DAY), '15:00', '17:00', 'ACTIVA'),
    ('RES-000007', '444', 'Taller de Robótica',
     DATE_SUB(CURDATE(), INTERVAL 5 DAY), '10:00', '12:00', 'CANCELADA');


INSERT INTO reserva_recursos (reserva_id, recurso_id) VALUES

    ('RES-000001', '1001'),
    ('RES-000001', '2001'),
    ('RES-000001', '3001'),
    ('RES-000001', '4001'),

    ('RES-000002', '1001'),
    ('RES-000002', '2001'),

    ('RES-000003', '1002'),
    ('RES-000003', '2002'),

    ('RES-000004', '2001'),
    ('RES-000004', '3002'),

    ('RES-000005', '1001'),
    ('RES-000005', '2002'),
    ('RES-000005', '3001'),

    ('RES-000006', '4001'),
    ('RES-000006', '4002'),

    ('RES-000007', '1001'),
    ('RES-000007', '2001');

COMMIT;
