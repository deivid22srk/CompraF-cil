import 'react-native-gesture-handler';
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { MD3LightTheme as DefaultTheme, PaperProvider } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import LoginScreen from './src/screens/LoginScreen';
import HomeScreen from './src/screens/HomeScreen';
import ProductDetailScreen from './src/screens/ProductDetailScreen';

const Stack = createStackNavigator();

const theme = {
  ...DefaultTheme,
  roundness: 12,
  colors: {
    ...DefaultTheme.colors,
    primary: '#2196F3',
    secondary: '#00B0FF',
    background: '#F8F9FA',
    surface: '#FFFFFF',
    error: '#B00020',
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
            headerTintColor: '#212529',
          }}
        >
          <Stack.Screen
            name="Login"
            component={LoginScreen}
            options={{ headerShown: false }}
          />
          <Stack.Screen
            name="Home"
            component={HomeScreen}
            options={{ title: 'CompraFácil', headerLeft: () => null }}
          />
          <Stack.Screen
            name="ProductDetail"
            component={ProductDetailScreen}
            options={{ title: 'Detalhes' }}
          />
        </Stack.Navigator>
      </NavigationContainer>
    </PaperProvider>
  );
}
