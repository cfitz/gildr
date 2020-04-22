# Guildr Application

### Features:
- Build in Kotlin using Ktor (https://ktor.io/)
- A namespaced RESTful API
- JWT Token authentication
- No database needed ( all data is stored as JSON in a .data directory )

### Requirements:

- Java 8 or higher


### Overview:

| Implemented  | PATH   | ACTION   | RESULT  | JWT required  |
|---|---|---|---|---|
| [x] | /login  | POST | Generate a JWT  |   |
| [x]  | /v1/players  | GET | List all players | * |
| [x]  | /v1/players   | POST | Create a new player |   |
| [x]  | /v1/players/:id  | GET | Show a player profile | * |
| [x]  | /v1/guilds  | GET | List all guilds (option q param for search by name )   | * |
| [x]  | /v1/guilds  | POST | Create a guild  | *   |
| [x]  | /v1/guilds/:id  | GET | Show a guild profile  | * |
| [x]  | /v1/guilds/:id  | DELETE | Disband a guild  | * |
| [x]  | /v1/guilds/:id/join  | PATCH | User request to join a guild  | * |
| [x]  | /v1/guilds/:id/leave  | PATCH | User request to leave a guild  | * |
| [x]  | /v1/guilds/:id/admin  | PATCH | Owner request to add members/owners  | * |
 


### Quickstart:

First unzip the distribution:
```bash
$  unzip gildr-0.0.1.zip
$ cd gildr-0.0.1
```

Then run it locally...
```
$ ./bin/gildr
```

..or with docker:
```bash
$ docker build -t gildr .
$ docker run -m512M --cpus 2 -it -p 8080:8080 --rm gildr
```

The application should be available at [http://localhost:8080](http://localhost:8080)



### Some examples on how to interact with the application:

The following uses curl commands to create and edit records in the API.

#### Create a player

To create a player, you must submit a usersname and password:


```json
{
  "name": "player1",
  "password": "noob"
}
```

To do this via curl:

```bash
curl -X POST -H "Content-Type: application/json"  -d '{"player": {"name": "player1", "password": "noob"}}'  http://localhost:8080/v1/players
```

You should get a response that looks like:

```bash
{
  "type" : "player",
  "id" : "7de187b7-6a73-408e-8430-083cc385b622",
  "memberOf" : [ ],
  "ownerOf" : [ ],
  "name" : "player1",
  "password" : "QxUOscRnL0rkJDQMd5+IcRoNlYIKR5+gQkJWQ7OwivQ="
}
```

Before we do anything else, we need to log in.

#### Create a player

The appliction uses JWT ( JSON Web Tokens ) for authentication. To get, a token, we can login with the user we created above:

```bash
 curl -X POST -H "Content-Type: application/json" -d '{"name": "player1", "password": "noob" }'  http://localhost:8080/login
```

This will give the following response:

```bash
{
  "token" : "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoiOTY2YTIxMGUtM2Y3Ni00M2E5LTk5OWMtNzQ5ZGU2MzVkNGQ1In0.t5PU_dFTfNI_-6YkyGj3Uq227UNKqI63sW_DPuSwXbs"
}
```

To make the next steps easier, copy that token and set an variable to $TOKEN. Or use jq:

```bash
TOKEN=$(curl -X POST -H "Content-Type: application/json" -d '{"name": "player1", "password": "noob" }'  http://localhost:8080/login | jq -r .token)
```

#### Create a guild

We are finally ready to create a guild:

```bash
curl -X POST  -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}" -d '{"guild": {"name":"rad dads","description":"dads are cool."}}' http://localhost:8080/v1/guilds
```

Response:

```bash
{
  "id" : "3bdef52b-c048-49f0-9943-a8ca667322b2",
  "type" : "guild",
  "name" : "rad dads",
  "description" : "dads are cool.",
  "owners" : [ ],
  "ownerIds" : [ "7de187b7-6a73-408e-8430-083cc385b622" ],
  "members" : [ ],
  "memberIds" : [ ]
}
```

Note: All guilds must have one owner, but when we create a guild, the user associated to the request token is added as the owner.

Also, note that the "owners" and "members" fields are not resolved. To see the fully resolved record, we can look at the record's show page:

```bash
curl  -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}"  http://localhost:8080/v1/guilds/3bdef52b-c048-49f0-9943-a8ca667322b2
```

```bash
{
  "id" : "3bdef52b-c048-49f0-9943-a8ca667322b2",
  "type" : "guild",
  "name" : "rad dads",
  "description" : "dads are cool.",
  "owners" : [ {
    "type" : "player",
    "id" : "966a210e-3f76-43a9-999c-749de635d4d5",
    "memberOf" : [ ],
    "ownerOf" : [ ],
    "name" : "player1",
    "password" : "QxUOscRnL0rkJDQMd5+IcRoNlYIKR5+gQkJWQ7OwivQ="
  } ],
  "ownerIds" : [ "966a210e-3f76-43a9-999c-749de635d4d5" ],
  "members" : [ ],
  "memberIds" : [ ]
}
```

We can also list all our guilds in the /v1/guilds endpoint:

```bash
curl  -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}"  http://localhost:8080/v1/guilds/3bdef52b-c048-49f0-9943-a8ca667322b2
```

We can also very simple "starts-with" searches for guilds with a "q=" parameter:
```bash
curl  -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}"  http://localhost:8080/v1/guilds?q=r
```

And we can disband the guild by deleting it:

```bash
curl -X DELETE -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}"  http://localhost:8080/v1/guilds/3bdef52b-c048-49f0-9943-a8ca667322b2
```

#### Other Membership Options

Users can join or leave guilds:

```bash
curl  -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}" -X PATCH  http://localhost:8080/v1/guilds/d5149564-88a4-48da-8392-5bdf6c6660dd/join
```

```bash
curl  -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}" -X PATCH  http://localhost:8080/v1/guilds/d5149564-88a4-48da-8392-5bdf6c6660dd/leave
```

There must always be one owner in a guild. If an owner wants to leave, they must first desginate a new owner.  There is an /admin endpoint to handle this, which takes the following JSON:

```json
{
"owners": { "onboard": ["d5149564-88a4-48da-8392-5bdf6c6660d"], "deboard": [ "bdw149564-88a4-48da-8392-5bdf6c6660d"]},
"members": { "onboard": ["ssv5149564-88a4-48da-8392-5bdf6c6660d"], "deboard": [ "zoew149564-88a4-48da-8392-5bdf6c6660d"]},
}
```

These elements are optional. With these an admin can remove owners or members from the guild.

```bash
curl  -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}" -X PATCH -d '{ "members": { "onboard": ["b2918bde-0357-4ec4-a83c-b8e18b15bd89"]}}' http://localhost:8080/v1/guilds/3db99bf9-52a1-4b92-881d-b9092008d4dc/admin
```

```bash
curl  -H "Content-Type: application/json"  -H "Authorization: Bearer ${TOKEN}" -X PATCH -d '{ "members": { "deboard": ["b2918bde-0357-4ec4-a83c-b8e18b15bd89"]}}' http://localhost:8080/v1/guilds/3db99bf9-52a1-4b92-881d-b9092008d4dc/admin
```

### Developer Tasks

The project uses gradle:

```bash
$ ./gradlew test
$ ./gradlew build
```


