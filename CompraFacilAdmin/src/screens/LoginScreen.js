import React, { useState } from 'react';
import { StyleSheet, View, Alert } from 'react-native';
import { Text, Button, ActivityIndicator, Surface } from 'react-native-paper';
import { supabase } from '../lib/supabase';
import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';

WebBrowser.maybeCompleteAuthSession();

export default function LoginScreen({ navigation }) {
  const [loading, setLoading] = useState(false);

  const [request, response, promptAsync] = Google.useAuthRequest({
    androidClientId: 'YOUR_ANDROID_CLIENT_ID',
    iosClientId: 'YOUR_IOS_CLIENT_ID',
    webClientId: 'YOUR_WEB_CLIENT_ID',
  });

  async function signInWithGoogle() {
    setLoading(true);
    try {
      const result = await promptAsync();
      if (result?.type === 'success') {
        const { authentication } = result;
        const { data, error } = await supabase.auth.signInWithIdToken({
          provider: 'google',
          token: authentication.idToken,
        });
        if (error) throw error;
        navigation.navigate('Dashboard');
      }
    } catch (error) {
      Alert.alert('Erro', error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Surface style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>CompraFácil Admin</Text>
        <Text style={styles.subtitle}>Painel de Gerenciamento</Text>
      </View>

      <View style={styles.content}>
        <Button
          mode="contained"
          onPress={signInWithGoogle}
          disabled={loading || !request}
          style={styles.button}
          contentStyle={styles.buttonContent}
          icon="google"
          buttonColor="#333"
        >
          {loading ? <ActivityIndicator color="#fff" /> : 'Entrar com Google'}
        </Button>

        {/* Demo bypass - remove in production */}
        <Button
          mode="text"
          onPress={() => navigation.navigate('Dashboard')}
          style={{ marginTop: 10 }}
        >
          Demo Bypass (Remover em produção)
        </Button>
      </View>
    </Surface>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 20,
    backgroundColor: '#fff',
  },
  header: {
    alignItems: 'center',
    marginBottom: 50,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#333',
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 18,
    color: '#666',
    marginTop: 10,
    textAlign: 'center',
  },
  content: {
    width: '100%',
  },
  button: {
    borderRadius: 8,
    elevation: 2,
  },
  buttonContent: {
    height: 50,
  },
});
