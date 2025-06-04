import pymysql
from typing import List, Tuple, Optional

# MySQL 접속 설정
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "IUlove!3545!",
    "database": "term_db",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor
}


def insert_standard_term(standard_term: str, synonym_group_id: int, category: str, is_sensitive: int):
    """standard_term_info 테이블에 삽입 (기존 존재하면 무시)"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('''
                INSERT IGNORE INTO standard_term_info 
                (standard_term, synonym_group_id, category, is_sensitive)
                VALUES (%s, %s, %s, %s);
            ''', (standard_term, synonym_group_id, category, is_sensitive))
        conn.commit()
    finally:
        conn.close()

def insert_term(term: str, standard_term: str):
    """term_mapping 테이블에 삽입 (기존 존재하면 무시)"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('''
                INSERT IGNORE INTO term_mapping (term, standard_term)
                VALUES (%s, %s);
            ''', (term, standard_term))
        conn.commit()
    finally:
        conn.close()

def insert_new_term_if_high_similarity(new_term: str, matched_entry: dict):
    """
    high similarity 기준으로 term과 standard_term 삽입
    matched_entry = {
        'standard_term': '이름',
        'synonym_group_id': 2,
        'category': '식별정보',
        'is_sensitive': 1
    }
    """
    insert_standard_term(
        matched_entry['standard_term'],
        matched_entry['synonym_group_id'],
        matched_entry['category'],
        matched_entry['is_sensitive']
    )
    insert_term(new_term, matched_entry['standard_term'])

def connect_db():
    return pymysql.connect(**DB_CONFIG)


def get_standard_term(term: str) -> Optional[str]:
    """term으로 standard_term 반환"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('SELECT standard_term FROM term_mapping WHERE term = %s;', (term,))
            row = cursor.fetchone()
            return row["standard_term"] if row else None
    finally:
        conn.close()

def get_metadata_by_standard_term(standard_term: str) -> Optional[dict]:
    """standard_term에 대한 메타정보 반환"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('''
                SELECT standard_term, synonym_group_id, category, is_sensitive 
                FROM standard_term_info 
                WHERE standard_term = %s LIMIT 1;
            ''', (standard_term,))
            row = cursor.fetchone()
            return row
    finally:
        conn.close()

def get_terms_by_standard_term(standard_term: str) -> List[str]:
    """standard_term에 해당하는 모든 term 반환"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('SELECT term FROM term_mapping WHERE standard_term = %s;', (standard_term,))
            rows = cursor.fetchall()
            return [row["term"] for row in rows]
    finally:
        conn.close()

def get_all_terms_and_stds() -> List[dict]:
    """
    term과 standard_term, 그리고 해당 표준 용어의 is_sensitive 값 반환
    → JOIN으로 standard_term_info에서 가져옴
    """
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute('''
                SELECT 
                    tm.term, 
                    tm.standard_term, 
                    sti.category ,
                    sti.is_sensitive,
                    sti.synonym_group_id
                FROM term_mapping tm
                JOIN standard_term_info sti 
                ON tm.standard_term = sti.standard_term;
            ''')
            rows = cursor.fetchall()
            return rows
    finally:
        conn.close()



def insert_if_high_similarity_sbert(term: str, standard_term: str):
    """SBERT 기반 유사도가 기준치 이상이면 term을 term_mapping에 삽입"""
    conn = connect_db()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT COUNT(*) AS count FROM term_mapping WHERE term = %s AND standard_term = %s",
                (term, standard_term)
            )
            result = cursor.fetchone()
            if result and result["count"] == 0:
                cursor.execute(
                    "INSERT INTO term_mapping (term, standard_term) VALUES (%s, %s)",
                    (term, standard_term)
                )
                conn.commit()
                print(f"추가됨: ({term} → {standard_term})")
            else:
                print(f"이미 존재함: ({term} → {standard_term})")
    finally:
        conn.close()

if __name__ == "__main__":
    term = "성함"
    standard = get_standard_term(term)
    if standard:
        metadata = get_metadata_by_standard_term(standard)
        print(f"[표준화] '{term}' → '{standard}'")
        print(f"[메타정보] {metadata}")
    else:
        print(f"'{term}' 은(는) 도메인 사전에 없음.")