from typing import List
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class MatchingKeyRequest(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )

    dataset_info_list: List[str]
    pii: List[str]