import pandas as pd
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score
from sentence_transformers import SentenceTransformer, util
import matplotlib.pyplot as plt

# 1. 모델 및 데이터 로드
MODEL_PATH = "model/sbert_domain_model/"
model = SentenceTransformer(MODEL_PATH)
df = pd.read_csv('test_pairs_hardcase.csv')

# 2. 임베딩 계산
embeddings1 = model.encode(df['col1'].tolist(), convert_to_tensor=True)
embeddings2 = model.encode(df['col2'].tolist(), convert_to_tensor=True)

# 3. 유사도 계산
cos_scores = util.cos_sim(embeddings1, embeddings2).diagonal()

# 4. 여러 threshold로 성능 지표 계산
thresholds = [1.0, 0.90, 0.89, 0.88, 0.87, 0.86, 0.85, 0.84, 0.80, 0.79, 0.78, 0.77, 0.76, 0.75, 0.74, 0.73, 0.72, 0.71, 0.70, 0.69, 0.68, 0.67, 0.66, 0.65, 0.64, 0.63, 0.62, 0.61, 0.60]
results = []

y_true = df['label'].tolist()

for threshold in thresholds:
    y_pred = (cos_scores >= threshold).int().tolist()
    precision = precision_score(y_true, y_pred, zero_division=0)
    recall = recall_score(y_true, y_pred, zero_division=0)
    f1 = f1_score(y_true, y_pred, zero_division=0)
    accuracy = accuracy_score(y_true, y_pred)

    results.append({
        "Threshold": threshold,
        "Precision": round(precision, 4),
        "Recall": round(recall, 4),
        "F1 Score": round(f1, 4),
        "Accuracy": round(accuracy, 4)
    })

# 5. 결과 출력
result_df = pd.DataFrame(results)
print(result_df)

# 6. 그래프 시각화
plt.figure(figsize=(10, 6))
plt.plot(result_df['Threshold'], result_df['Precision'], marker='o', label='Precision')
plt.plot(result_df['Threshold'], result_df['Recall'], marker='s', label='Recall')
plt.plot(result_df['Threshold'], result_df['F1 Score'], marker='^', label='F1 Score')
plt.plot(result_df['Threshold'], result_df['Accuracy'], marker='x', label='Accuracy')

plt.title('Threshold vs Precision / Recall / F1 Score / Accuracy')
plt.xlabel('Threshold')
plt.ylabel('Score')
plt.xticks(result_df['Threshold'])
plt.ylim(0, 1.05)
plt.grid(True)
plt.legend()
plt.tight_layout()
plt.show()