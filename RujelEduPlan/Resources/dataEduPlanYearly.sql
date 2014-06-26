/* Часы учебного плана: за основу взят базисный учебный план для общего среднего образования */
INSERT INTO PL_HOURS (PH_ID,CYCLE_ID,WEEKLY_HOURS) VALUES 
 (1,1,7), (3,3,2), (8,8,2), (9,9,3), (7,7,3), (12,12,2), (11,11,4), (10,10,2), (5,5,7), 
 (6,6,2), (17,17,3), (16,16,3), (15,15,3), (14,14,3), (13,13,3), (21,21,2), (22,22,2),
 (20,20,2), (19,19,2), (18,18,2), (25,25,1), (26,26,1), (24,24,1), (23,23,1), (27,27,2),
 (28,28,2), (30,30,2), (29,29,1), (31,31,2), (38,38,2), (36,36,2), (33,33,2), (34,34,1),
 (32,32,2), (35,35,2), (37,37,2), (41,41,5), (39,39,5), (43,43,5), (42,42,5), (40,40,5),
 (45,45,2), (44,44,2), (50,50,2), (48,48,1), (46,46,2), (49,49,1), (47,47,2), (52,52,2),
 (51,51,2), (53,53,2), (54,54,1), (55,55,1), (56,56,2), (57,57,1), (61,61,2), (60,60,2),
 (59,59,2), (58,58,2), (62,62,2), (64,64,1), (74,74,3), (63,63,3), (73,73,3), (69,69,1),
 (68,68,3), (66,66,2), (67,67,4), (71,71,4), (70,70,2), (72,72,2), (65,65,2), (76,76,3),
 (78,78,3), (75,75,2), (77,77,2);

/* Каникулы и праздники */
INSERT INTO PL_HOLIDAY (H_ID, HOLIDAY_NAME, DATE_BEGIN, DATE_END) VALUES
(1,'Осенние каникулы','2013-11-04','2013-11-10'),
(2,'День народного единства','2013-11-04','2013-11-04'),
(3,'Зимние каникулы','2013-12-30','2014-01-12'),
(4,'День защитника отечества','2014-02-23','2014-02-23'),
(5,'Международный женский день','2014-03-08','2014-03-08'),
(6,'Весенние каникулы','2014-03-24','2014-03-30'),
(7,'День весны и труда','2014-05-01','2014-05-01'),
(8,'День Победы','2014-05-09','2014-05-09'),
(9,'День России','2014-06-12','2014-06-12');
