import { Stack } from 'expo-router';
import { PaperProvider, MD3LightTheme } from 'react-native-paper';

const theme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#6200ee',
    secondary: '#03dac6',
  },
};

export default function RootLayout() {
  return (
    <PaperProvider theme={theme}>
      <Stack screenOptions={{
        headerStyle: { backgroundColor: '#6200ee' },
        headerTintColor: '#fff',
        headerTitleStyle: { fontWeight: 'bold' },
      }}>
        <Stack.Screen name="index" options={{ title: 'CompraFácil' }} />
        <Stack.Screen name="login" options={{ title: 'Login', headerShown: false }} />
        <Stack.Screen name="home" options={{ title: 'Início' }} />
        <Stack.Screen name="product/[id]" options={{ title: 'Produto' }} />
        <Stack.Screen name="cart" options={{ title: 'Carrinho' }} />
      </Stack>
    </PaperProvider>
  );
}
