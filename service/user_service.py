import asyncio
import base64
import json
from typing import Dict, List
from fastapi import UploadFile

from py_client.message.message import Message
from py_client.agent import Agent
from utils import CLIENT_ID, HOST, PORT

class UserService:
    counter = 0

    async def convert_file(self, file: UploadFile) -> str | None:
        header = {
            "file.name": file.filename or "unknown",
            "api.id": self.get_api_id()
        }

        if (response := await self.run_async(topic_name="convert", partition=1, header=header, payload=await file.read())) is None:
            return None
        
        return response.payload.decode("utf-8")
 
    async def export_file(self, format: str, file_name: str):
        header = {
            "file.format": format,
            "file.name": file_name,
            "api.id": self.get_api_id()
        }

        if (response := await self.run_async(topic_name="convert", partition=2, header=header)) is None:
            return None
        
        return response.payload

    async def find_candidate_columns(self, files: List[UploadFile]) -> Dict[str, List[str]] | None:
        header = {
            "api.id": self.get_api_id()
        }
        payload = json.dumps({
            file.filename: base64.b64encode(await file.read()).decode("utf-8")
            for file in files
        })
        
        if (response := await self.run_async(topic_name="join", partition=1, header=header, payload=payload)) is None:
            return None
        
        return json.loads(response.payload.decode("utf-8"))
        
    async def create_project(self, project_name: str, candidate_columns: Dict[str, List[str]], files: List[UploadFile]) -> str | None:
        header = {
            "api.id": self.get_api_id()
        }
        payload = json.dumps({
            "projectName": project_name,
            "candidateColumns": candidate_columns,
            "files": {
                file.filename: base64.b64encode(await file.read()).decode("utf-8")
                for file in files
            }
        })

        if (response := await self.run_async(topic_name="join", partition=2, header=header, payload=payload)) is None:
            return None
        
        return response.payload.decode("utf-8")

    async def get_projects(self) -> str | None:
        header = {
            "api.id": self.get_api_id()
        }

        if(response := await self.run_async(topic_name="join", partition=3, header=header)) is None:
            return None
        
        return response.payload.decode("utf-8")
        
    async def create_ci(self, project_id: str) -> bool:
        header = {
            "project.id": project_id,
            "api.id": self.get_api_id()
        }

        if (response := await self.run_async(topic_name="join", partition=4, header=header)) is None:
            return False
        
        return True
    
    async def join(self, project_id: str) -> bool:
        header = {
            "project.id": project_id,
            "api.id": self.get_api_id()
        }

        if (response := await self.run_async(topic_name="join", partition=5, header=header)) is None:
            return False
        
        return True

    async def get_project(self, project_id: str) -> str | None:
        header = {
            "project.id": project_id,
            "api.id": self.get_api_id()
        }

        if (response := await self.run_async(topic_name="join", partition=6, header=header)) is None:
            return None
        
        return response.payload.decode("utf-8")
    
    async def get_result(self, project_id: str) -> bytes | None:
        header = {
            "project.id": project_id,
            "api.id": self.get_api_id()
        }

        if (response := await self.run_async(topic_name="join", partition=7, header=header)) is None:
            return None
        
        return response.payload

    def consume(self, topic_name: str, partition: int, header: Dict[str, str], payload = None, condition: Dict[str, str] | None = None) -> Message | None:
        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header, payload=payload)
            if error := response.get_header("error"):
                print(f"? consume(): {error}")
                return None
            
            condition = header if condition is None else condition
            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=condition):
                return None
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? consume(): {error}")
                return None
            
            return response

    def get_api_id(self) -> str:
        api_id = str(UserService.counter)
        UserService.counter += 1

        return api_id
    
    def run_async(self, topic_name: str, partition: int, header: Dict[str, str], payload = None):
        return asyncio.get_running_loop().run_in_executor(None, self.consume, topic_name, partition, header, payload)