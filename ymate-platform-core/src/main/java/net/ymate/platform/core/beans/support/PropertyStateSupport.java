/*
 * Copyright 2007-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ymate.platform.core.beans.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.ymate.platform.core.beans.annotation.PropertyState;
import net.ymate.platform.core.util.ClassUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author 刘镇 (suninformation@163.com) on 16/7/3 上午2:05
 * @version 1.0
 */
public class PropertyStateSupport<T> {

    private final Class<?> __targetClass;

    private Map<String, PropertyStateMeta> __propertyStates;

    public static <T> PropertyStateSupport<T> create(T original) throws Exception {
        return new PropertyStateSupport<T>(original);
    }

    public PropertyStateSupport(T original) throws Exception {
        __targetClass = original.getClass();
        __propertyStates = new HashMap<String, PropertyStateMeta>();
        //
        ClassUtils.BeanWrapper<T> _wrapper = ClassUtils.wrapper(original);
        for (String _fieldName : _wrapper.getFieldNames()) {
            PropertyState _state = _wrapper.getField(_fieldName).getAnnotation(PropertyState.class);
            if (_state != null) {
                __propertyStates.put(StringUtils.defaultIfBlank(_state.setterName(), "set" + StringUtils.capitalize(_fieldName)),
                        new PropertyStateMeta(StringUtils.defaultIfBlank(_state.propertyName(), _fieldName), _wrapper.getValue(_fieldName)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public T bind() {
        return (T) Enhancer.create(__targetClass, new MethodInterceptor() {
            public Object intercept(Object targetObject, Method targetMethod, Object[] methodParams, MethodProxy methodProxy) throws Throwable {
                Object _result = methodProxy.invokeSuper(targetObject, methodParams);
                PropertyStateMeta _meta = __propertyStates.get(targetMethod.getName());
                if (_meta != null) {
                    _meta.setNewValue(methodParams != null ? methodParams[0] : null);
                }
                return _result;
            }
        });
    }

    public String[] getChangedPropertyNames() {
        Set<String> _states = new HashSet<String>();
        for (PropertyStateMeta _meta : __propertyStates.values()) {
            if (_meta.isChanged()) {
                _states.add(_meta.getPropertyName());
            }
        }
        return _states.toArray(new String[_states.size()]);
    }

    public Set<PropertyStateMeta> getChangedProperties() {
        Set<PropertyStateMeta> _states = new HashSet<PropertyStateMeta>();
        for (PropertyStateMeta _meta : __propertyStates.values()) {
            if (_meta.isChanged()) {
                _states.add(_meta);
            }
        }
        return _states;
    }

    public static class PropertyStateMeta {
        private String propertyName;
        private Object originalValue;
        private Object newValue;

        private boolean changed;

        public PropertyStateMeta(String propertyName, Object originalValue) {
            this.propertyName = propertyName;
            this.originalValue = originalValue;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Object getOriginalValue() {
            return originalValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public void setNewValue(Object newValue) {
            this.newValue = newValue;
            if (originalValue != null) {
                changed = !originalValue.equals(newValue);
            } else {
                changed = newValue != null;
            }
        }

        public boolean isChanged() {
            return changed;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this, SerializerFeature.WriteMapNullValue);
        }
    }
}
