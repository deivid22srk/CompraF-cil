import 'react-native-gesture-handler';
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import LoginScreen from './src/screens/LoginScreen';
import DashboardScreen from './src/screens/DashboardScreen';
import AddProductScreen from './src/screens/AddProductScreen';

const Stack = createStackNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Login">
        <Stack.Screen
          name="Login"
          component={LoginScreen}
          options={{ headerShown: false }}
        />
        <Stack.Screen
          name="Dashboard"
          component={DashboardScreen}
          options={{ title: 'Painel Admin' }}
        />
        <Stack.Screen
          name="AddProduct"
          component={AddProductScreen}
          options={({ route }) => ({
            title: route.params?.product ? 'Editar Produto' : 'Novo Produto'
          })}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
