from sentence_transformers import SentenceTransformer, models, InputExample, losses
from torch.utils.data import DataLoader
from itertools import combinations, product

# 1. 클러스터 정의
term_clusters = {
    "이름": ["이름", "성명", "성함", "fullname", "name", "고객명", "이용자명", "회원명"],
    "주민번호": ["주민번호", "주민등록번호", "주민등록 식별번호", "resident number", "ssn", "id number"],
    "주소": ["주소", "거주지", "주소지", "거소지", "address", "residence", "location"],
    "전화번호": ["전화번호", "휴대폰번호", "휴대전화번호", "핸드폰번호", "phone", "phonenumber", "mobile"],
    "이메일": ["이메일", "전자우편", "email", "e-mail", "mail address"],
    "생년월일": ["생일", "생년월일", "birthdate", "birthday", "출생일", "dob", "date of birth"],
    "성별": ["성별", "gender", "sex"],
    "연락처": ["연락처", "연락처번호", "연락처정보", "contact", "contact number", "contact info"]
}

# 2. 비식별 단어 (이름/번호 포함되지만 식별정보 아님)
neg_terms = [
    "제품명", "상품명", "모델명", "품명", "규격명", "기기명", "기계명", "지점명", "지사명",
    "캠퍼스명", "매장명", "건물명", "층명", "교실명", "부서명", "기관명", "회사명", "사업명", "과목명", "학과명",
    "프로그램명", "파일명", "DB명", "테이블명", "변수명", "함수명", "색상명", "품목명",
    "차량명", "이벤트명", "일정명", "계정명", "역할명", "옵션명", "항목명", "카테고리명",
    "문서번호", "주문번호", "고객번호", "사번", "학번", "수강번호", "과목번호", "송장번호",
    "상품번호", "바코드번호", "게시글번호", "계정번호", "유저번호", "오류번호", "메시지번호",
    "이벤트번호", "트랜잭션번호", "시리얼번호", "우편번호", "건물번호", "좌석번호", "순번",
    "줄번호", "채널번호", "row번호", "index번호", "id번호", "테이블번호", "필드번호"
]

# 3. 학습 샘플 생성 함수
def generate_samples(term_clusters, neg_terms):
    positive_samples = []
    negative_samples = []

    # (1) Positive samples: 동일 클러스터 내 유사어
    for synonyms in term_clusters.values():
        for t1, t2 in combinations(synonyms, 2):
            positive_samples.append(InputExample(texts=[t1, t2], label=1.0))

    # (2) Negative samples: 서로 다른 클러스터 간
    cluster_list = list(term_clusters.values())
    for i in range(len(cluster_list)):
        for j in range(i + 1, len(cluster_list)):
            for t1, t2 in product(cluster_list[i], cluster_list[j]):
                negative_samples.append(InputExample(texts=[t1, t2], label=0.0))

    # (3) Negative samples: 식별 용어와 비식별 용어 조합
    for cluster_terms in term_clusters.values():
        for t1 in cluster_terms:
            for t2 in neg_terms:
                negative_samples.append(InputExample(texts=[t1, t2], label=0.0))

    return positive_samples + negative_samples

# 4. 전체 샘플 생성
all_samples = generate_samples(term_clusters, neg_terms)

# 5. DataLoader 구성
train_dataloader = DataLoader(all_samples, shuffle=True, batch_size=16)

# 6. SBERT 모델 초기화
word_embedding_model = word_embedding_model = models.Transformer("snunlp/KR-SBERT-V40K-klueNLI-augSTS")
pooling_model = models.Pooling(word_embedding_model.get_word_embedding_dimension())
model = SentenceTransformer(modules=[word_embedding_model, pooling_model])
train_loss = losses.CosineSimilarityLoss(model)

# 7. 모델 학습
model.fit(
    train_objectives=[(train_dataloader, train_loss)],
    epochs=5,
    warmup_steps=10,
    show_progress_bar=True
)

# 8. 모델 저장
model.save('model/sbert_domain_model/')