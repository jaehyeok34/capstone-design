from dataclasses import dataclass
from typing import List
from dataclasses_json import LetterCase, dataclass_json


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class PiiDetectionResponse:
    dataset_info_list: List[str]
    pii: List[str]
    