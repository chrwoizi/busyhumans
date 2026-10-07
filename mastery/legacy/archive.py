"""Reads a `mongodump --archive` file of the old MongoDB database.

Only the BSON types that the old application wrote are supported. This needs
nothing but the standard library, so the MongoDB driver is not a dependency.
"""
import datetime
import gzip
import struct
import uuid
from hashlib import md5

MAGIC = 0x8199E26D
TERMINATOR = b"\xff\xff\xff\xff"

UUID_LEGACY = 3
UUID_STANDARD = 4


class Binary:
    def __init__(self, subtype, data):
        self.subtype = subtype
        self.data = data

    def __eq__(self, other):
        return isinstance(other, Binary) and (self.subtype, self.data) == (other.subtype, other.data)

    def __hash__(self):
        return hash((self.subtype, self.data))


class ObjectId:
    def __init__(self, data):
        self.data = data

    def __eq__(self, other):
        return isinstance(other, ObjectId) and self.data == other.data

    def __hash__(self):
        return hash(self.data)


def to_uuid(value):
    """Converts a stored id to the id that the old application used.

    The old application read ids in the legacy format by hashing their bytes
    (java.util.UUID.nameUUIDFromBytes). Those hashed ids are the public ones, the
    other collections reference them.
    """
    if value is None:
        return None
    if value.subtype == UUID_STANDARD:
        return uuid.UUID(bytes=value.data)
    if value.subtype == UUID_LEGACY:
        return uuid.UUID(bytes=md5(value.data).digest(), version=3)
    raise ValueError("Binary of subtype %d is not an id" % value.subtype)


def _cstring(buf, pos):
    end = buf.index(b"\x00", pos)
    return buf[pos:end].decode("utf-8"), end + 1


def decode_document(buf, pos=0, as_array=False):
    (size,) = struct.unpack_from("<i", buf, pos)
    end = pos + size - 1
    pos += 4
    result = [] if as_array else {}
    while pos < end:
        typ = buf[pos]
        key, pos = _cstring(buf, pos + 1)
        if typ == 0x01:
            (value,) = struct.unpack_from("<d", buf, pos)
            pos += 8
        elif typ == 0x02:
            (length,) = struct.unpack_from("<i", buf, pos)
            value = buf[pos + 4:pos + 4 + length - 1].decode("utf-8")
            pos += 4 + length
        elif typ in (0x03, 0x04):
            (length,) = struct.unpack_from("<i", buf, pos)
            value = decode_document(buf, pos, as_array=typ == 0x04)
            pos += length
        elif typ == 0x05:
            (length,) = struct.unpack_from("<i", buf, pos)
            value = Binary(buf[pos + 4], bytes(buf[pos + 5:pos + 5 + length]))
            pos += 5 + length
        elif typ == 0x07:
            value = ObjectId(bytes(buf[pos:pos + 12]))
            pos += 12
        elif typ == 0x08:
            value = buf[pos] == 1
            pos += 1
        elif typ == 0x09:
            (millis,) = struct.unpack_from("<q", buf, pos)
            value = datetime.datetime.fromtimestamp(millis / 1000, datetime.UTC)
            pos += 8
        elif typ == 0x0A:
            value = None
        elif typ == 0x10:
            (value,) = struct.unpack_from("<i", buf, pos)
            pos += 4
        elif typ == 0x12:
            (value,) = struct.unpack_from("<q", buf, pos)
            pos += 8
        else:
            raise ValueError("Unsupported BSON type 0x%02x of field '%s'" % (typ, key))
        if as_array:
            result.append(value)
        else:
            result[key] = value
    return result


def read_archive(path):
    """Yields (collection name, document) for every document in the archive."""
    with open(path, "rb") as f:
        buf = f.read()
    if buf[:2] == b"\x1f\x8b":
        buf = gzip.decompress(buf)

    (magic,) = struct.unpack_from("<I", buf, 0)
    if magic != MAGIC:
        raise ValueError("'%s' is not a mongodump archive" % path)

    # The prelude describes the collections. Skip it, the blocks name them again.
    pos = 4
    while buf[pos:pos + 4] != TERMINATOR:
        (size,) = struct.unpack_from("<i", buf, pos)
        pos += size
    pos += 4

    # Blocks: a header that names the collection, its documents, a terminator.
    while pos < len(buf):
        (size,) = struct.unpack_from("<i", buf, pos)
        header = decode_document(buf, pos)
        pos += size
        while buf[pos:pos + 4] != TERMINATOR:
            (size,) = struct.unpack_from("<i", buf, pos)
            yield header["collection"], decode_document(buf, pos)
            pos += size
        pos += 4


def load_collections(path):
    """Returns {collection name: [documents]}."""
    collections = {}
    for name, document in read_archive(path):
        collections.setdefault(name, []).append(document)
    return collections
