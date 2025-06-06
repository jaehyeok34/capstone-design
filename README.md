
# main.py 전체 흐름

    1. 도메인 사전 기반 판단 (domain_search.py)
        1. 파일들을 순회하며 컬럼명 추출 → 표준화 시도
        2. 사전 매칭된 경우 → 식별정보로 판단 (outputA)
        3. 표준화 실패 컬럼 → 후속 단계로 넘김(outputB)

    2. 임베딩 기반 판단
        1. outputB에 대해 SBERT유사도 기반으로 도메인 사전항목과 의미 유사도 비교
        2. 유사도 기준 이상인 경우 → 식별정보로 판단 (outputC)
        3. 기준 미달 항목 → 후속 단계로 넘김 (outputD)

    3. 카디널리티 기반 판단
        1. outputD에 해당하는 컬럼을 파일별로 모아 데이터의 고유값 비율 측정
        2. 고유값 비율이 기준 이상인 경우 식별정보로 판단


    
    1. 입력으로 들어온 컬럼명 표준화 시도 -> 도메인 사전에 매핑 시도 -> 판단된 컬럼, 그렇지 않은 컬럼 분류
    2. 판단되지 않은 컬럼 존재 시 임베딩 판단 시도 -> 
    3. " 카디널리티 기반 판단 시도 ->
----------
## db_utils.py

1. table 구성은 다음과 같음.
    1. **term_mapping** - [standard_term, synonym_group_id, category, is_sensitive]
    2. **standard_term_info** -
    [term, standard_term]

2. 함수 설명
    1. get_standard_term(term: str) 
         -> term_mapping table에서 표준용어를 반환함
    2. get_metadata_by_standard_term(standard_term: str)
        -> standard_term_info에서 메타정보를 반환함

-------------
        
## domain_search.py
흐름:
1. 폴더 내 파일에서 컬럼명(+파일이름)을 추출함
2. 컬럼명 전처리 → 표준화시도
    1. 표준화 성공 → outputA + 메타정보(← standard_term_info)와 함께 저장
        1. ** standard_term_info table**은 다음과 같음 : standard_term, synonym_group_id,category,is_sensitive
        2. 표준화 실패 → outputB ⇒ 후속처리 대상
3. 함수설명
    1. extract_columns_from_folder(folder_path: str)
        -> 폴더 내의 파일들을 열어서 컬럼명을 추출하고, 파일명과 컬럼명 쌍을 저장함.
    2. clean_column_names(columns: List[str])
        -> 컬럼명을 표준화함: 소문자 통일, 공백제거, 특수문자 제거
    3. flatten_list(nested)
        ->현재 코드에서는 사용되지 않았으나 향후 컬럼 리스트를 병합. 중첩 제거시 사용될 수도 있음
    4. process_columns(columns_info: List[Dict[str, str]])
        1. 전처리된 컬럼명을 도메인 사전 함수의 standard_term(표준화명)으로 표준화함.
        2. 표준화성공시, 메타데이터 추가하고, outputA에 저장
        3. 표준화실패시, outputB에 저장
-----------
## embeding_search.py
흐름:
1.  domain_search.py의 outputB(칼럼명) →SBERT
2. SBERT: 도메인 사전의 표준용어(standard_term)과 유사도 측정
3. 유사도 기준치 이상 → outputC저장 → DB자동 삽입(term_mapping table)
4. 유사도 기준치 미만 → outputD저장 : 후속처리 진행 위함

5. 함수 설명 - find_similar_terms()
*  입력 및 표준 용어 임베딩
    **input_embeddings = model.encode(input_terms, convert_to_tensor=True)**
        * input_term리스트를 SBERT모델로 인코딩하여 문장 임베딩 벡터로 변환
    **std_terms = list({entry["standard_term"] for entry in domain_entries})**
        * 도메인 사전에서 표준 용어들만 추출한 리스트 생성
    **std_embeddings = model.encode(std_terms, convert_to_tensor=True)**
		* 도메인 사전의 standard_term들도 SBERT로 임베딩

--------------
## cardinality_cheak.py
1. 함수설명
    1. is_potential_identifiear():
        * 고유값 비율(**min_ratio**) 기준으로 식별정보 여부 판단
        * cardinality_ratio: (L/M)
            * L: NAN을 제외한 고유값 개수
            * N: 전체행 개수
        * min_ratio보다 cardinality_ratio가 높으면 식별정보로 판단
    2. adjust_threshold_by_join_rate()
        * 추후 합의 알고리즘과 통합후 수정 예정
        입력: join_rate: 결합률
        출력: min_ratio: 조정값 반환
    3. analyze_dataframe()
        * join_rate가 주어지면 기준값을 동적으로 조절
        * is_potential_identifier()을 호출하여 판별함
        * 식별정보로 판단 -> identifier_columns에 추가함
---------
이외 about_model 디렉토리에는 SBERT threshold산출 결과와 model 훈련데이터 생성 코드가 있음