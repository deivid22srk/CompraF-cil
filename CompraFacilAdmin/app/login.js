import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Button, Text, Title } from 'react-native-paper';
import * as WebBrowser from 'expo-web-browser';
import * as Google from 'expo-auth-session/providers/google';
import { supabase } from '../src/lib/supabase';

WebBrowser.maybeCompleteAuthSession();

export default function Login() {
  const [request, response, promptAsync] = Google.useAuthRequest({
    androidClientId: "YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com",
    iosClientId: "YOUR_IOS_CLIENT_ID.apps.googleusercontent.com",
    webClientId: "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
  });

  React.useEffect(() => {
    if (response?.type === 'success') {
      const { authentication } = response;
      handleGoogleLogin(authentication.idToken);
    }
  }, [response]);

  async function handleGoogleLogin(idToken) {
    const { data, error } = await supabase.auth.signInWithIdToken({
      provider: 'google',
      token: idToken,
    });
    if (error) console.log('Error logging in:', error.message);
  }

  async function handleMockLogin() {
    const { data, error } = await supabase.auth.signInWithPassword({
      email: 'admin@example.com',
      password: 'adminpassword',
    });
    if (error) {
       await supabase.auth.signUp({
         email: 'admin@example.com',
         password: 'adminpassword',
       });
    }
  }

  return (
    <View style={styles.container}>
      <Title style={styles.title}>Painel Admin</Title>
      <Text style={styles.subtitle}>Gerencie sua loja CompraFácil</Text>

      <Button
        mode="contained"
        onPress={() => promptAsync()}
        disabled={!request}
        icon="google"
        style={styles.button}
      >
        Entrar com Google
      </Button>

      <Button
        mode="outlined"
        onPress={handleMockLogin}
        style={styles.button}
      >
        Entrar como Admin (Demo)
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#007bff',
  },
  subtitle: {
    fontSize: 18,
    marginBottom: 40,
    color: '#666',
  },
  button: {
    width: '100%',
    marginVertical: 10,
  }
});
