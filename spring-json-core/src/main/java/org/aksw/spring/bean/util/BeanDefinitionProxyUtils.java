package org.aksw.spring.bean.util;

import java.beans.PropertyDescriptor;
import java.util.Map;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;

public class BeanDefinitionProxyUtils {
    /**
     * Take an existing bean (pojo), and use it as a template for creating a corresponding
     * bean definition (based on its properties and values) and register it
     * with the specfied scope at the given registry.
     *
     *
     * @param ctx
     * @param bean
     * @param scopeName
     * @param propertyToSpel Spel expression to evaluate on the scope
     * @return
     */
    public static <T> T createScopedProxy(BeanFactory ctx, T bean, String scopeName, Map<String, Object> propertyToSpel) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)((ConfigurableApplicationContext)ctx).getBeanFactory();
        //BeanDefinitionRegistry registry = (BeanDefinitionRegistry)ctx; // ConfigurableApplicationContext

        Class<?> beanClazz = bean.getClass();

        RootBeanDefinition beanDef = new RootBeanDefinition(beanClazz);
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(bean);
        MutablePropertyValues propertyValues = beanDef.getPropertyValues();

        for(PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
            String propertyName = pd.getName();
            boolean isReadable = beanWrapper.isReadableProperty(propertyName);
            boolean isWritable = beanWrapper.isWritableProperty(propertyName);
            if(isReadable && isWritable) {
                Object value = beanWrapper.getPropertyValue(propertyName);

                // TODO Make this configurable
                if(value != null && value instanceof String) {
                    String text = (String)value;
                    if(text.startsWith("##")) {
                        value = "#{ " + text.substring(2) + " }";
                    }
                }

                propertyValues.add(propertyName, value);
            }
        }
        beanDef.setScope(scopeName);

        //DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)((ConfigurableApplicationContext)ctx).getBeanFactory();
        //String rootName = BeanDefinitionReaderUtils.generateBeanName(beanDef, beanFactory);
        String beanName = BeanDefinitionReaderUtils.registerWithGeneratedName(beanDef, beanFactory);

        BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(beanDef, beanName), beanFactory, true);
        BeanDefinitionReaderUtils.registerBeanDefinition(proxyHolder, beanFactory);

        String proxyName = proxyHolder.getBeanName();
        @SuppressWarnings("unchecked")
        T result = (T)beanFactory.getBean(proxyName);

        return result;
    }

}
