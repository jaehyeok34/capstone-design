import random
import pandas as pd

# 1. 유사 쌍 생성용 도메인 기반 그룹
similar_pairs = {
    "이름": ["이름", "성명", "성함", "fullname", "name", "고객명", "이용자명", "회원명"],
    "주민번호": ["주민번호", "주민등록번호", "주민등록 식별번호", "resident number", "ssn", "id number"],
    "주소": ["주소", "거주지", "주소지", "거소지", "address", "residence", "location"],
    "전화번호": ["전화번호", "휴대폰번호", "휴대전화번호", "핸드폰번호", "phone", "phonenumber", "mobile"],
    "이메일": ["이메일", "전자우편", "email", "e-mail", "mail address"],
    "생년월일": ["생일", "생년월일", "birthdate", "birthday", "출생일", "dob", "date of birth"],
    "성별": ["성별", "gender", "sex"],
    "연락처": ["연락처", "연락처번호", "연락처정보", "contact", "contact number", "contact info"]
}

# 2. 비식별 단어 리스트
dissimilar_terms = [
    "제품명", "상품명", "모델명", "품명", "규격명", "기기명", "기계명", "지점명", "지사명",
    "캠퍼스명", "매장명", "건물명", "층명", "교실명", "부서명", "기관명", "회사명", "사업명", "과목명", "학과명",
    "프로그램명", "파일명", "DB명", "테이블명", "변수명", "함수명", "색상명", "품목명",
    "차량명", "이벤트명", "일정명", "계정명", "역할명", "옵션명", "항목명", "카테고리명",
    "문서번호", "주문번호", "고객번호", "사번", "학번", "수강번호", "과목번호", "송장번호",
    "상품번호", "바코드번호", "게시글번호", "계정번호", "유저번호", "오류번호", "메시지번호",
    "이벤트번호", "트랜잭션번호", "시리얼번호", "우편번호", "건물번호", "좌석번호", "순번",
    "줄번호", "채널번호", "row번호", "index번호", "id번호", "테이블번호", "필드번호"
]

flat_similar_terms = [term for sublist in similar_pairs.values() for term in sublist]

hard_negatives_raw = [
    ("이름", "제품명"),
    ("주소", "주소코드"),
    ("전화번호", "전화기모델"),
    ("이메일", "메일템플릿"),
    ("성별", "성별코드"),
    ("생일", "이벤트일"),
    ("주민번호", "주문번호"),
    ("연락처", "연락처메모")
]

# 4. 노이즈 삽입 함수 정의
def add_noise(term):
    noise_types = [
        lambda x: x.lower(),                        # 소문자
        lambda x: x.upper(),                        # 대문자
        lambda x: x.replace(" ", "_"),              # 언더스코어
        lambda x: x.replace(" ", ""),               # 공백 제거
        lambda x: f"{x}_1",                         # 숫자 접미사
        lambda x: f"{x}정보",                        # 의미적 접미어
    ]
    return random.choice(noise_types)(term)

# 5. 샘플 생성
def generate_samples():
    data = []

    # 유사 쌍: 300개
    for _ in range(300):
        domain_terms = random.choice(list(similar_pairs.values()))
        a, b = random.sample(domain_terms, 2)
        data.append((a, b, 1))

    # Easy 비유사 쌍: 300개
    for _ in range(300):
        a = random.choice(flat_similar_terms)
        b = random.choice(dissimilar_terms)
        data.append((a, b, 0))

    # Hard negative: 200개
    for _ in range(200):
        a, b = random.choice(hard_negatives_raw)
        data.append((a, b, 0))

    # 노이즈 유사쌍: 200개
    for _ in range(200):
        domain_terms = random.choice(list(similar_pairs.values()))
        base = random.choice(domain_terms)
        noisy = add_noise(base)
        data.append((base, noisy, 1))

    # 섞기
    random.shuffle(data)
    return data

# 6. 저장
dataset = generate_samples()
df = pd.DataFrame(dataset, columns=["col1", "col2", "label"])
df.to_csv("test_pairs_hardcase.csv", index=False, encoding="utf-8-sig")
print("✅ test_pairs_hardcase.csv 생성 완료 (hard negative 포함)")