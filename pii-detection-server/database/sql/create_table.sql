-- 1. 테이블 생성
CREATE TABLE standard_term_info (
    standard_term VARCHAR(100) PRIMARY KEY,
    synonym_group_id INT NOT NULL,
    category VARCHAR(50),
    is_sensitive TINYINT(1)
);

CREATE TABLE term_mapping (
    term VARCHAR(100) PRIMARY KEY,
    standard_term VARCHAR(100) NOT NULL,
    FOREIGN KEY (standard_term) REFERENCES standard_term_info(standard_term)
);