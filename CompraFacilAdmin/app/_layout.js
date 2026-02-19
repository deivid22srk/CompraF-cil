import { Stack } from 'expo-router';
import { PaperProvider, MD3LightTheme } from 'react-native-paper';

const theme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#007bff',
    secondary: '#6c757d',
  },
};

export default function RootLayout() {
  return (
    <PaperProvider theme={theme}>
      <Stack screenOptions={{
        headerStyle: { backgroundColor: '#007bff' },
        headerTintColor: '#fff',
        headerTitleStyle: { fontWeight: 'bold' },
      }}>
        <Stack.Screen name="index" options={{ title: 'Admin CompraFácil' }} />
        <Stack.Screen name="login" options={{ title: 'Login Admin', headerShown: false }} />
        <Stack.Screen name="dashboard" options={{ title: 'Painel de Controle' }} />
        <Stack.Screen name="add-product" options={{ title: 'Novo Produto' }} />
      </Stack>
    </PaperProvider>
  );
}
