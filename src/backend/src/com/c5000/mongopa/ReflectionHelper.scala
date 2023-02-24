package com.c5000.mongopa

import internal.FieldDesc
import java.lang.reflect.Field
import org.joda.time.DateTime
import java.util

object ReflectionHelper {

    def getAllFields(clazz: Class[_]): List[Field] = {
        if (clazz.getSuperclass != null) {
            return clazz.getDeclaredFields.toList.union(getAllFields(clazz.getSuperclass))
        }
        else {
            return clazz.getDeclaredFields.toList
        }
    }

    def isValidGetter(field: Field, getter: java.lang.reflect.Method): Boolean = {
        return getter.getReturnType == field.getType && getter.getParameterTypes.isEmpty
    }

    def isValidSetter(field: Field, setter: java.lang.reflect.Method): Boolean = {
        return setter.getReturnType.getName == "void" && setter.getParameterTypes.size == 1 && setter.getParameterTypes.head == field.getType
    }

    def findGetter(field: Field): java.lang.reflect.Method = {
        var getter = field.getDeclaringClass.getMethods.find(x => x.getName == field.getName)
        if (getter.isDefined && isValidGetter(field, getter.get))
            return getter.get
        else {
            val getterName = "get" + field.getName.substring(0, 1).toUpperCase + field.getName.substring(1)
            getter = field.getDeclaringClass.getMethods.find(x => x.getName == getterName)
            if (getter.isDefined && isValidGetter(field, getter.get))
                return getter.get
        }
        return null
    }

    def findSetter(field: Field): java.lang.reflect.Method = {
        var setterName = field.getName + "_$eq"
        var setter = field.getDeclaringClass.getMethods.find(x => x.getName == setterName)
        if (setter.isDefined && isValidSetter(field, setter.get))
            return setter.get
        else {
            setterName = "set" + field.getName.substring(0, 1).toUpperCase + field.getName.substring(1)
            setter = field.getDeclaringClass.getMethods.find(x => x.getName == setterName)
            if (setter.isDefined && isValidSetter(field, setter.get))
                return setter.get
        }
        return null
    }

    def getFieldValue(desc: FieldDesc, instance: Any): Object = {
        var result: Object = null
        if (desc.getter != null)
            result = desc.getter.invoke(instance)
        else
            result = desc.field.get(instance)
        if(result.isInstanceOf[DateTime])
            result = result.asInstanceOf[DateTime].toDate
        return result
    }

    def setFieldValue(desc: FieldDesc, instance: Any, value: Object) {
        var converted = value
        if(desc.field.getType == classOf[DateTime])
            converted = if(value == null) null else new DateTime(value.asInstanceOf[util.Date])
        if (desc.setter != null)
            desc.setter.invoke(instance, converted)
        else
            desc.field.set(instance, converted)
    }

}
