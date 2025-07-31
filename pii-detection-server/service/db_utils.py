from typing import Dict, List, Optional
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
    

def get_metadata(standard_term: str) -> Optional[Dict]:
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
    
    
def get_domain() -> List[Dict]:
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
        return rows


def get_all_standard_term() -> List[Dict]:
    conn = __get_db()
    with conn.cursor() as cursor:
        cursor.execute('SELECT standard_term FROM standard_term_info')
        rows = cursor.fetchall()
        return [row["standard_term"] for row in rows] if rows else []
    

def insert_new_term(new_term: str, entity: Dict):
    __insert_standard_term(entity)
    __insert_term(new_term, entity["standard_term"])


def __insert_standard_term(entity: Dict):
    standard_term, synonym_group_id, category, is_sensitive = (
        entity["standard_term"],
        entity["synonym_group_id"],
        entity["category"],
        entity["is_sensitive"]
    )

    conn = __get_db()
    with conn.cursor() as cursor:
        cursor.execute('''
            INSERT IGNORE INTO standard_term_info 
            (standard_term, synonym_group_id, category, is_sensitive)
            VALUES (%s, %s, %s, %s);
        ''', (standard_term, synonym_group_id, category, is_sensitive))

    conn.commit()


def __insert_term(term: str, standard_term: str):
    conn = __get_db()
    with conn.cursor() as cursor:
        cursor.execute('''
            INSERT IGNORE INTO term_mapping (term, standard_term)
            VALUES (%s, %s);
        ''', (term, standard_term))

    conn.commit()