import base64
import json
from typing import Dict, List
from fastapi import UploadFile

from py_client.agent import Agent
from utils import CLIENT_ID, HOST, PORT

class UserService:
    counter = 0

    async def convert_file(self, file: UploadFile) -> str | None:
        topic_name = "convert"
        partition = 1

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = UserService.counter
            UserService.counter += 1

            header = {
                "file.name": file.filename or "unknown",
                "api.id": str(api_id)
            }

            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header, payload=await file.read())
            if error := response.get_header("error"):
                print(f"? convert_file(): {error}")
                return None

            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):   
                return None
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? convert_file(): {error}")
                return None
            
            return response.payload.decode("utf-8")
 
    async def export_file(self, format: str, file_name: str):
        topic_name = "convert"
        partition = 2

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = UserService.counter
            UserService.counter += 1

            header = {
                "file.format": format,
                "file.name": file_name,
                "api.id": str(api_id)
            }

            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header, payload=b"req")
            if error := response.get_header("error"):
                print(f"? export_file(): {error}")
                return None

            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):
                return None
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? export_file(): {error}")
                return None
            
            return response.payload

    async def find_candidate_columns(self, files: List[UploadFile]) -> Dict[str, List[str]] | None:
        topic_name = "join"
        partition = 1

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = str(UserService.counter)
            UserService.counter += 1

            header = {
                "api.id": api_id
            }

            payload = json.dumps({
                file.filename: base64.b64encode(await file.read()).decode("utf-8")
                for file in files
            })

            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header, payload=payload)
            if error := response.get_header("error"):
                print(f"? find_candidate_columns(): {error}")
                return None
            
            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):
                return None
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? find_candidate_columns(): {error}")
                return None
            
            return json.loads(response.payload.decode("utf-8"))
        
    async def create_project(self, project_name: str, candidate_columns: Dict[str, List[str]], files: List[UploadFile]) -> str | None:
        topic_name = "join"
        partition = 2

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = str(UserService.counter)
            UserService.counter += 1

            header = {"api.id": api_id}
            payload = json.dumps({
                "projectName": project_name,
                "candidateColumns": candidate_columns,
                "files": {
                    file.filename: base64.b64encode(await file.read()).decode("utf-8")
                    for file in files
                }
            })

            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header, payload=payload)
            if error := response.get_header("error"):
                print(f"? create_project(): {error}")
                return None
            
            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):
                return None
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? create_project(): {error}")
                return None
            
            return response.payload.decode("utf-8")

    async def get_projects(self) -> str | None:
        topic_name = "join"
        partition = 3

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = str(UserService.counter)
            UserService.counter += 1

            header = {"api.id": api_id}
            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header)
            if error := response.get_header("error"):
                print(f"? get_projects(): {error}")
                return None
            
            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):
                return None
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? get_projects(): {error}")
                return None
            
            return response.payload.decode("utf-8")
        
    async def create_ci(self, project_id: str) -> bool:
        topic_name = "join"
        partition = 4

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = str(UserService.counter)
            UserService.counter += 1

            header = {
                "project.id": project_id,
                "api.id": api_id
            }

            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header)
            if error := response.get_header("error"):
                print(f"? create_ci(): {error}")
                return False
            
            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):
                return False
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? create_ci(): {error}")
                return False
            
        return True
    
    async def get_project(self, project_id: str) -> str | None:
        topic_name = "join"
        partition = 6

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = str(UserService.counter)
            UserService.counter += 1

            header = {
                "project.id": project_id,
                "api.id": api_id
            }

            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header)
            if error := response.get_header("error"):
                print(f"? get_project(): {error}")
                return None
            
            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):
                return None
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? get_project(): {error}")
                return None
            
            return response.payload.decode("utf-8")
        
    async def join(self, project_id: str) -> bool:
        topic_name = "join"
        partition = 5

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = str(UserService.counter)
            UserService.counter += 1

            header = {
                "project.id": project_id,
                "api.id": api_id
            }

            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header)
            if error := response.get_header("error"):
                print(f"? join(): {error}")
                return False
            
            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):
                return False
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? join(): {error}")
                return False
            
        return True
    
    async def get_result(self, project_id: str) -> bytes | None:
        topic_name = "join"
        partition = 7

        with Agent.of(host=HOST, port=PORT, client_id=CLIENT_ID) as agent:
            api_id = str(UserService.counter)
            UserService.counter += 1

            header = {
                "project.id": project_id,
                "api.id": api_id
            }

            response = agent.producer.syncProduce(topic_name=topic_name, partition=str(partition), header=header)
            if error := response.get_header("error"):
                print(f"? get_result(): {error}")
                return None
            
            if not agent.find_and_seek(topic_name=topic_name, partition=str(-partition), condition=header):
                return None
            
            response = agent.consumer.consume(topic_name=topic_name, partition=str(-partition))[0]
            if error := response.get_header("error"):
                print(f"? get_result(): {error}")
                return None
            
            return response.payload