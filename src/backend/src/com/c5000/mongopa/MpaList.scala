package com.c5000.mongopa

import collection.mutable.Buffer

class MpaList[ItemType <: MpaModelBase] {

    protected[mongopa] var _items: Buffer[ItemType] = Buffer()
    protected[mongopa] var _validItems: Buffer[ItemType] = Buffer()

    def this(createFrom: Iterable[ItemType]) {
        this()
        _items = createFrom.toBuffer
        updateDeletedFlags()
    }

    /**
     * @return iterable of all items including deleted
     */
    def allItems = _items

    /**
     * @return iterable of items that are not deleted
     */
    def validItems = _validItems

    def add(item: ItemType) {
        _items += item
        _validItems += item
    }

    def setDeleted(item: ItemType) {
        _items.filter(_.equals(item)).foreach(_.deleted = true)
        updateDeletedFlags();
    }

    def updateDeletedFlags() {
        _validItems = _items.filter(!_.deleted)
    }
}
