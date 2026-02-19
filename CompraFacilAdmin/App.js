import 'react-native-gesture-handler';
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { MD3LightTheme as DefaultTheme, PaperProvider } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import LoginScreen from './src/screens/LoginScreen';
import DashboardScreen from './src/screens/DashboardScreen';
import AddProductScreen from './src/screens/AddProductScreen';

const Stack = createStackNavigator();

const theme = {
  ...DefaultTheme,
  roundness: 12,
  colors: {
    ...DefaultTheme.colors,
    primary: '#343A40',
    secondary: '#4CAF50',
    background: '#F8F9FA',
    surface: '#FFFFFF',
    error: '#DC3545',
  },
};

export default function App() {
  return (
    <PaperProvider
      theme={theme}
      settings={{
        icon: props => <MaterialCommunityIcons {...props} />,
      }}
    >
      <NavigationContainer>
        <Stack.Navigator
          initialRouteName="Login"
          screenOptions={{
            headerStyle: { backgroundColor: '#FFFFFF' },
            headerTitleStyle: { fontWeight: '700', fontSize: 18 },
            headerShadowVisible: false,
            headerTintColor: '#343A40',
          }}
        >
          <Stack.Screen
            name="Login"
            component={LoginScreen}
            options={{ headerShown: false }}
          />
          <Stack.Screen
            name="Dashboard"
            component={DashboardScreen}
            options={{ title: 'Painel Lojista', headerLeft: () => null }}
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
    </PaperProvider>
  );
}
