import React, { useState } from 'react';
import { StyleSheet, View, Image } from 'react-native';
import { Text, Button, ActivityIndicator, Surface, useTheme } from 'react-native-paper';
import { supabase } from '../lib/supabase';
import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';

WebBrowser.maybeCompleteAuthSession();

export default function LoginScreen({ navigation }) {
  const [loading, setLoading] = useState(false);
  const theme = useTheme();

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
        navigation.navigate('Home');
      }
    } catch (error) {
      console.error('Login Error:', error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Surface style={styles.container}>
      <View style={styles.header}>
        <Text variant="displaySmall" style={[styles.title, { color: theme.colors.primary }]}>CompraFácil</Text>
        <Text variant="titleMedium" style={styles.subtitle}>Sua loja local favorita</Text>
      </View>

      <View style={styles.content}>
        <Button
          mode="contained"
          onPress={signInWithGoogle}
          disabled={loading || !request}
          style={styles.button}
          contentStyle={styles.buttonContent}
          icon="google"
        >
          {loading ? <ActivityIndicator color="#fff" /> : 'Entrar com Google'}
        </Button>

        <Button
          mode="outlined"
          onPress={() => navigation.navigate('Home')}
          style={[styles.skipButton, { borderColor: theme.colors.primary }]}
          textColor={theme.colors.primary}
        >
          Explorar sem conta
        </Button>
      </View>
    </Surface>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#FFFFFF',
  },
  header: {
    alignItems: 'center',
    marginBottom: 60,
  },
  title: {
    fontWeight: '900',
  },
  subtitle: {
    color: '#6C757D',
    marginTop: 8,
    textAlign: 'center',
  },
  content: {
    width: '100%',
  },
  button: {
    borderRadius: 14,
    elevation: 0,
  },
  buttonContent: {
    height: 56,
  },
  skipButton: {
    marginTop: 16,
    borderRadius: 14,
    borderWidth: 1.5,
  },
});
