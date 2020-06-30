# ProjectManagementApp

Running application:
```sh
$ sbt dockerComposeUp
```
By default API will be available on port 8080

Running tests:
```sh
$ sbt dockerComposeTest
```

### Api specification

---

API is authenticated using JWT token. Example token:
`Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwidXNlcklkIjoiZjdhNjdjNDAtYjRjMi0xMWVhLWIzZGUtMDI0MmFjMTMwMDA2IiwiaWF0IjoxNTE2MjM5MDIyfQ.94Rp7zSq9amh-l4r7QihSSFfVZ9trIuF9emCHa32Nv4`

---

`POST /projects`

Body:
```json
{
   "name": "test_name"
}
```

Response:

```json
{
    "id": 1,
    "name": "test_name",
    "author": "f7a67c40-b4c2-11ea-b3de-0242ac130006",
    "created_on": "2020-06-30T12:31:45.733502"
}
```

---

`PUT /projects/{id}`

Body:
```json
{
   "name": "new_test_name"
}
```

Response empty


---

`GET /projects/{id}`

Response:
```json
{
    "project": {
        "id": 1,
        "name": "new_test_name",
        "author": "f7a67c40-b4c2-11ea-b3de-0242ac130006",
        "created_on": "2020-06-30T12:31:45.733502"
    },
    "tasks": [],
    "totalTime": "PT0S"
}
```

---

`DELETE /projects/{id}`

Response empty

---

`POST /projects/query`

Body:
```json
{
	      "ids": ["1"],
	      "from": "2020-06-01T10:43:25.25114",
	      "to": "2020-07-01T11:43:25.25114",
		  "page": 0,
		  "deleted": false,
		  "size": 10,
		  "order": "asc", // or "desc"
          "sortBy": "creationTime" // or "updateTime"
}
```
Response:
```json
{
    "projects": [
        {
            "project": {
                "id": 1,
                "name": "test_name",
                "author": "f7a67c40-b4c2-11ea-b3de-0242ac130006",
                "createdOn": "2020-06-30T13:25:07.432576",
                "deletedOn": null
            },
            "tasks": [
                {
                    "id": 1,
                    "projectId": 1,
                    "startTime": "2020-06-30T10:43:25.25114",
                    "endTime": "2020-06-30T11:43:25.25114",
                    "author": "f7a67c40-b4c2-11ea-b3de-0242ac130006",
                    "comment": "some comment",
                    "volume": 10,
                    "deletedOn": null
                }
            ],
            "totalTime": "PT1H"
        }
    ]
}
```
---

`POST /tasks`

Body:
```json
{
	      "projectId": "2",
	      "startTime": "2020-06-30T10:43:25.25114",
	      "endTime": "2020-06-30T11:43:25.25114",
		  "comment": "some comment",
		  "volume": 10
}
```
Response:
```json
{
    "id": 1,
    "projectId": 2,
    "startTime": "2020-06-30T10:43:25.25114",
    "endTime": "2020-06-30T11:43:25.25114",
    "author": "f7a67c40-b4c2-11ea-b3de-0242ac130006",
    "comment": "some comment",
    "volume": 10
}
```

---

`PUT /tasks/{id}`

Body:
```json
{
	      "startTime": "2020-06-30T10:43:25.25114",
	      "endTime": "2020-06-30T11:43:25.25114",
		  "comment": "some comment",
		  "volume": 10
}
```
Response:
```json
{
    "id": 2,
    "projectId": 1,
    "startTime": "2020-06-30T10:43:25.25114",
    "endTime": "2020-06-30T11:43:25.25114",
    "author": "f7a67c40-b4c2-11ea-b3de-0242ac130006",
    "comment": "some comment",
    "volume": 10
}
```

---

`DELETE /tasks/{id}`

Response empty

---

`POST /statistics`

Body:
```json
{
	      "users" : ["f7a67c40-b4c2-11ea-b3de-0242ac130006"],
	      "from": "2019-01",
	      "to": "2020-12"
}
```
Respone:
```json
{
    "statistics": [
        {
            "userId": "f7a67c40-b4c2-11ea-b3de-0242ac130006",
            "numberOfTasks": 1,
            "numberOfTasksWithVolume": 1,
            "averageDuration": "PT1H",
            "averageVolume": 10,
            "averageDurationPerVolume": "PT6M"
        }
    ]
}
```