# db_utils.py

import pymysql
from typing import List, Tuple, Optional
import mysql.connector

# MySQL 접속 설정
DB_CONFIG = {

    "host": "localhost",
    "user": "root",
    "password": "IUlove!3545!",
    "database": "domain_dict_db",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor
}

def connect_db():
    return pymysql.connect(**DB_CONFIG)


def insert_term(term: str, standard_term: str, synonym_group_id: int, category: str, language: str , is_sensitive: int = 1):
    """신규 term 삽입 (기존에 없을 경우만)"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('''
                INSERT IGNORE INTO domain_dictionary (term, standard_term, synonym_group_id, category, language, is_sensitive)
                VALUES (%s, %s, %s, %s, %s, %s);
            ''', (term, standard_term, synonym_group_id, category, language, is_sensitive))
        conn.commit()
    finally:
        conn.close()

def get_term_info(term: str) -> Optional[dict]:
    """term으로 전체 정보 가져오기"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('SELECT * FROM domain_dictionary WHERE term = %s;', (term,))
            row = cursor.fetchone()
            return row
    finally:
        conn.close()

def get_all_terms() -> List[str]:
    """등록된 모든 term 반환"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('SELECT term FROM domain_dictionary;')
            rows = cursor.fetchall()
            return [row["term"] for row in rows]
    finally:
        conn.close()

def get_term_standard_mapping() -> List[Tuple[str, str]]:
    """term과 standard_term 쌍 리스트"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('SELECT term, standard_term FROM domain_dictionary;')
            rows = cursor.fetchall()
            return [(row["term"], row["standard_term"]) for row in rows]
    finally:
        conn.close()

#standard_term으로 -> 모든 term 반환
def get_terms_by_standard_term(standard_term: str) -> List[str]:
    """standard_term으로 묶인 모든 term 반환"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('SELECT term FROM domain_dictionary WHERE standard_term = %s;', (standard_term,))
            rows = cursor.fetchall()
            return [row["term"] for row in rows]
    finally:
        conn.close()

def get_standard_term(term: str) -> Optional[str]:
    """term으로 standard_term 반환"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('SELECT standard_term FROM domain_dictionary WHERE term = %s;', (term,))
            row = cursor.fetchone()
            return row["standard_term"] if row else None
    finally:
        conn.close()

def get_metadata_by_standard_term(standard_term: str) -> Optional[dict]:
    """
    표준화된 standard_term을 기준으로 category, language, is_sensitive 등 메타데이터 반환
    (term은 제외)
    """
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('''
                SELECT standard_term, synonym_group_id, category, language, is_sensitive 
                FROM domain_dictionary 
                WHERE standard_term = %s LIMIT 1;
            ''', (standard_term,))
            row = cursor.fetchone()
            return row
    finally:
        conn.close()

def get_all_terms_and_stds():
    # domain_dictionary 테이블에서 term과 standard_term 가져오기
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT term, standard_term, category, is_sensitive, language FROM domain_dictionary")
            return cursor.fetchall()
    finally:
        conn.close()

def insert_new_term_if_high_similarity(new_term: str, matched_entry: dict):
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                INSERT INTO domain_dictionary (term, standard_term, synonym_group_id, category, language, is_sensitive)
                VALUES (%s, %s, %s, %s, %s, %s)
                """,
                (
                    new_term,
                    matched_entry['standard_term'],
                    matched_entry.get('synonym_group_id', 0),  # 필요 시 기본값 0
                    matched_entry['category'],
                    matched_entry['language'],
                    matched_entry['is_sensitive']
                )
            )
        conn.commit()
    finally:
        conn.close()

if __name__ == "__main__":

    term = "성함"

    standard = get_standard_term(term)  # → "이름"
    if standard:
        metadata = get_metadata_by_standard_term(standard)
        print(f"[표준화] '{term}' → '{standard}'")
        print(f"[메타정보] {metadata}")
    else:
        print(f"'{term}' 은(는) 도메인 사전에 없음.")

