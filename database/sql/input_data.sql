-- standard_term_info 데이터 삽입
INSERT INTO standard_term_info (standard_term, synonym_group_id, category, is_sensitive)
VALUES
('주민번호', 1, '식별정보', TRUE),
('이름', 2, '식별정보', TRUE),
('전화번호', 3, '식별정보', TRUE),
('주소', 4, '식별정보', TRUE),
('생년월일', 5, '식별정보', TRUE),
('연락처', 6, '식별정보', TRUE),
('성별', 7, '식별정보', TRUE);

-- term_mapping 데이터 삽입
INSERT INTO term_mapping (term, standard_term)
VALUES
-- 주민번호
('주민등록번호', '주민번호'),
('idnumber', '주민번호'),
('ssn', '주민번호'),
('resident_registration_number', '주민번호'),

-- 이름
('성명', '이름'),
('fullname', '이름'),
('name', '이름'),
('성함', '이름'),
('고객명', '이름'),

-- 전화번호
('phone_number', '전화번호'),
('휴대전화', '전화번호'),
('휴대번호', '전화번호'),
('휴대전화번호', '전화번호'),
('phone', '전화번호'),

-- 주소
('거주지', '주소'),
('address', '주소'),
('residence', '주소'),
('location', '주소'),

-- 생년월일
('생년월일', '생년월일'),
('birth', '생년월일'),
('birth_date', '생년월일'),
('date_of_birth', '생년월일');