curl -u elastic:Qwe54188_ -X DELETE "http://localhost:9200/user_v1"

curl -u elastic:Qwe54188_ -X PUT "http://127.0.0.1:9200/user_v1" \
  -H 'Content-Type: application/json' \
  -d '{
    "aliases": {
      "user": {}
    },
    "mappings": {
      "properties": {
        "account": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "index": true
        },
        "wx_union": {
          "type": "keyword",
          "index": true
        },
        "mp_open": {
          "type": "keyword",
          "index": true
        },
        "email": {
          "type": "keyword",
          "index": true
        },
        "phone": {
          "type": "keyword",
          "index": true
        },
        "ident": {
          "type": "keyword",
          "index": true
        },
        "passwd": {
          "type": "keyword",
          "index": false
        },
        "avatar": {
          "type": "keyword",
          "index": false
        },
        "tags": {
          "type": "keyword",
          "index": true
        },
        "nick": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "index": true
        },
        "name": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "index": true
        },
        "profile": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "index": true
        },
        "birthday": {
          "type": "date",
          "format": "yyyy-MM-dd",
          "index": true
        },
        "country": {
          "type": "keyword",
          "index": true
        },
        "address": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart",
          "index": true
        },
        "role": {
          "type": "integer",
          "index": true
        },
        "level": {
          "type": "integer",
          "index": true
        },
        "gender": {
          "type": "integer",
          "index": true
        },
        "deleted": {
          "type": "keyword",
          "index": true
        },
        "create_time": {
          "type": "date",
          "format": "yyyy-MM-dd HH:mm:ss"
          "index": true
        },
        "update_time": {
          "type": "date",
          "format": "yyyy-MM-dd HH:mm:ss"
          "index": true
        }
      }
    }
  }'

curl -u elastic:Qwe54188_ -X GET "http://127.0.0.1:9200/user_v1/_mapping?pretty"
