from typing import List, Optional
from flask import current_app, g
import pymysql


def __get_db():
    if 'db' not in g:
        g.db = pymysql.connect(**current_app.config['DB_CONFIG'])

    return g.db


def get_standard_term(term: str) -> Optional[str]:
    """term으로 standard_term 반환"""
    conn = __get_db()
    with conn.cursor() as cursor:
        cursor.execute('SELECT standard_term FROM term_mapping WHERE term = %s;', (term,))
        row = cursor.fetchone()
        return row["standard_term"] if row else None
    

def get_metadata(standard_term: str) -> Optional[dict]:
    """standard_term에 대한 메타정보 반환"""
    conn = __get_db()
    with conn.cursor() as cursor:
        cursor.execute('''
            SELECT standard_term, synonym_group_id, category, is_sensitive 
            FROM standard_term_info 
            WHERE standard_term = %s LIMIT 1;
        ''', (standard_term,))
        row = cursor.fetchone()
        return row if row else None
    

def get_all_standard_term() -> List[dict]:
    """
    term과 standard_term, 그리고 해당 표준 용어의 is_sensitive 값 반환
    → JOIN으로 standard_term_info에서 가져옴
    """
    conn = __get_db()
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
        return rows if rows else None