from PIL import ImageGrab
from qreader import QReader
from clipboard import copy
import numpy


def make_error(msg: str, code: int):
    return {"error": {"msg": msg, "code": code}}


REQ_RECOGNIZE = "1"
REQ_EXIT = "2"

ERR_NO_IMAGE = "e:1"
ERR_NO_CODE = "e:2"
ERR_MANY_CODES = "e:3"

reader = QReader()


def recognize():
    img = ImageGrab.grabclipboard()
    if img is None:
        return ERR_NO_IMAGE
    matches = reader.detect_and_decode(numpy.array(img))
    if len(matches) == 0:
        return ERR_NO_CODE
    if len(matches) != 1:
        return ERR_MANY_CODES
    copy(matches[0])
    return f"r:{matches[0]}"


while 1:
    i = input()
    if i == REQ_RECOGNIZE:
        print(recognize())
    elif i == REQ_EXIT:
        print()
        break
    else:
        continue
