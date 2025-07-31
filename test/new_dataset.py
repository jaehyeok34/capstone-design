import pandas as pd
import random

# 기존 데이터 불러오기
df = pd.read_csv('data.csv')

# 자동차 번호판 생성 함수
def generate_car_plate():
    num1 = str(random.randint(10, 999))
    char = random.choice(['가', '나', '다', '라', '마', '바', '사'])
    num2 = str(random.randint(1000, 9999))
    return f"{num1}{char}{num2}"

# 핸드폰 기종 리스트
phone_models = [
    'iPhone 14', 'iPhone 13', 'iPhone 12', 'iPhone 11', 'iPhone XR', 'iPhone XS', 'iPhone X', 'iPhone 8',
    'Galaxy S23', 'Galaxy S22', 'Galaxy S21', 'Galaxy S20', 'Galaxy S10', 'Galaxy Note 20', 'Galaxy Note 10',
    'Galaxy Z Flip', 'Galaxy Z Fold', 'Galaxy A52', 'Galaxy A32',
    'Pixel 8', 'Pixel 7', 'Pixel 6', 'Pixel 5', 'Pixel 4',
    'LG Wing', 'LG Velvet', 'LG V60', 'LG G8',
    'Xiaomi Mi 11', 'Xiaomi Redmi Note 10', 'Xiaomi Poco F3',
    'OnePlus 9', 'OnePlus 8T',
    'Sony Xperia 1 III', 'Sony Xperia 5 II',
    'Huawei P50', 'Huawei Mate 40',
    'Oppo Find X3', 'Oppo Reno 6',
    'Vivo X60', 'Vivo Y72',
    'Realme GT', 'Realme 8',
    'Motorola Edge 20', 'Motorola Moto G Power',
    'iPhone SE', 'Galaxy M12', 'Pixel 4a', 'LG Q92', 'Galaxy A12', 'Redmi 9', 'Galaxy S9', 'iPhone 7', 'iPhone 6S'
]

# 주거 형태 리스트
residence_types = ['아파트', '오피스텔', '단독주택', '다가구', '연립주택', '기숙사', '원룸', '빌라']

# 이메일 도메인 리스트
domain_list = ['gmail.com', 'naver.com', 'daum.net', 'yahoo.com', 'icloud.com', 'hotmail.com', 'outlook.com']

def generate_email_provider():
    return random.choice(domain_list)

# 직업군 리스트
job_types = ['학생', '회사원', '자영업', '프리랜서', '공무원', '교사', '의사', '간호사', '연구원', '개발자', '마케터', '디자이너']

# 혈액형 리스트
blood_types = ['A', 'B', 'O', 'AB']

# 선호 색상 리스트
favorite_colors = ['빨강', '파랑', '초록', '노랑', '검정', '흰색', '보라', '주황', '분홍', '회색']

# 결혼 여부 리스트
marital_statuses = ['미혼', '기혼', '이혼', '사별']

# 반려동물 종류 리스트
pet_types = ['없음', '강아지', '고양이', '새', '물고기', '햄스터', '파충류']

# 차량 종류 리스트
vehicle_types = ['승용차', 'SUV', '트럭', '오토바이', '전기차', '버스', '자전거']

# 가족 구성원 수 리스트
family_sizes = [1, 2, 3, 4, 5, 6]

# 취미 리스트
hobbies = ['독서', '운동', '영화감상', '음악감상', '게임', '요리', '여행', '사진', '등산', '쇼핑', '수영', '자전거']

# 출생 연도 리스트 (1980~2010)
birth_years = list(range(1980, 2011))

# 통신사 리스트
mobile_carriers = ['SKT', 'KT', 'LG U+', '알뜰폰']

# SNS 사용 여부 리스트
sns_usage = ['사용', '미사용']

# 새로운 컬럼 추가
df['car_plate'] = [generate_car_plate() for _ in range(len(df))]
df['phone_model'] = [random.choice(phone_models) for _ in range(len(df))]
df['residence_type'] = [random.choice(residence_types) for _ in range(len(df))]
df['job_type'] = [random.choice(job_types) for _ in range(len(df))]
df['blood_type'] = [random.choice(blood_types) for _ in range(len(df))]
df['favorite_color'] = [random.choice(favorite_colors) for _ in range(len(df))]
df['marital_status'] = [random.choice(marital_statuses) for _ in range(len(df))]
df['pet_type'] = [random.choice(pet_types) for _ in range(len(df))]
df['vehicle_type'] = [random.choice(vehicle_types) for _ in range(len(df))]
df['family_size'] = [random.choice(family_sizes) for _ in range(len(df))]
df['hobby'] = [random.choice(hobbies) for _ in range(len(df))]
df['birth_year'] = [random.choice(birth_years) for _ in range(len(df))]
df['mobile_carrier'] = [random.choice(mobile_carriers) for _ in range(len(df))]
df['sns_usage'] = [random.choice(sns_usage) for _ in range(len(df))]

# income, monthly_spending 컬럼이 존재하면 삭제
for col in ['income', 'monthly_spending']:
    if col in df.columns:
        df.drop(columns=[col], inplace=True)

# 새로운 데이터셋 저장
df.to_csv('new_data.csv', index=False)

print('새로운 컬럼이 추가된 데이터셋이 new_data.csv로 저장되었습니다.')
