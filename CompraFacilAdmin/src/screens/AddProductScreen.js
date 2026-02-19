import React, { useState, useEffect } from 'react';
import { StyleSheet, View, ScrollView, Image, Alert, Dimensions } from 'react-native';
import { TextInput, Button, ActivityIndicator, Surface, Text, useTheme, IconButton, Chip } from 'react-native-paper';
import * as ImagePicker from 'expo-image-picker';
import { supabase } from '../lib/supabase';
import { decode } from 'base64-arraybuffer';

const { width } = Dimensions.get('window');

export default function AddProductScreen({ route, navigation }) {
  const editingProduct = route.params?.product;
  const theme = useTheme();

  const [name, setName] = useState(editingProduct?.name || '');
  const [description, setDescription] = useState(editingProduct?.description || '');
  const [price, setPrice] = useState(editingProduct?.price?.toString() || '');
  const [image, setImage] = useState(editingProduct?.image_url || null);
  const [imageBase64, setImageBase64] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(editingProduct?.category_id || null);
  const [loadingCategories, setLoadingCategories] = useState(true);

  useEffect(() => {
    fetchCategories();
  }, []);

  async function fetchCategories() {
    try {
      const { data, error } = await supabase.from('categories').select('*').order('name');
      if (error) throw error;
      setCategories(data);
    } catch (error) {
      console.error('Categories Error:', error.message);
    } finally {
      setLoadingCategories(false);
    }
  }

  const pickImage = async () => {
    let result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.5,
      base64: true,
    });

    if (!result.canceled) {
      setImage(result.assets[0].uri);
      setImageBase64(result.assets[0].base64);
    }
  };

  async function handleSave() {
    if (!name || !price) {
      Alert.alert('Campos Obrigatórios', 'Por favor, preencha o nome e o preço.');
      return;
    }

    setUploading(true);
    try {
      let imageUrl = image;

      if (imageBase64) {
        const fileName = `prod_${Date.now()}.jpg`;
        const { error: uploadError } = await supabase.storage
          .from('product-images')
          .upload(fileName, decode(imageBase64), {
            contentType: 'image/jpeg'
          });

        if (uploadError) throw uploadError;

        const { data: publicUrlData } = supabase.storage
          .from('product-images')
          .getPublicUrl(fileName);

        imageUrl = publicUrlData.publicUrl;
      }

      const productData = {
        name,
        description,
        price: parseFloat(price.replace(',', '.')),
        image_url: imageUrl,
        category_id: selectedCategory,
      };

      if (editingProduct) {
        const { error } = await supabase
          .from('products')
          .update(productData)
          .eq('id', editingProduct.id);
        if (error) throw error;
      } else {
        const { error } = await supabase
          .from('products')
          .insert([productData]);
        if (error) throw error;
      }

      Alert.alert('Sucesso', 'Produto salvo com sucesso!');
      navigation.goBack();
    } catch (error) {
      Alert.alert('Erro ao Salvar', error.message);
    } finally {
      setUploading(false);
    }
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
      <Surface style={styles.card} elevation={1}>
        <TextInput
          label="Nome do Produto"
          value={name}
          onChangeText={setName}
          mode="outlined"
          style={styles.input}
          outlineStyle={styles.outline}
        />

        <TextInput
          label="Preço de Venda"
          value={price}
          onChangeText={setPrice}
          mode="outlined"
          keyboardType="decimal-pad"
          style={styles.input}
          outlineStyle={styles.outline}
          left={<TextInput.Affix text="R$ " />}
        />

        <Text variant="titleMedium" style={styles.label}>Categoria</Text>
        <View style={styles.categoriesContainer}>
          {loadingCategories ? (
            <ActivityIndicator size="small" />
          ) : (
            categories.map((cat) => (
              <Chip
                key={cat.id}
                selected={selectedCategory === cat.id}
                onPress={() => setSelectedCategory(cat.id)}
                style={styles.chip}
                showSelectedOverlay
                mode="flat"
              >
                {cat.name}
              </Chip>
            ))
          )}
        </View>

        <TextInput
          label="Descrição"
          value={description}
          onChangeText={setDescription}
          mode="outlined"
          multiline
          numberOfLines={3}
          style={styles.input}
          outlineStyle={styles.outline}
        />

        <Text variant="titleMedium" style={styles.label}>Foto</Text>
        <View style={styles.imageContainer}>
          {image ? (
            <Image source={{ uri: image }} style={styles.preview} />
          ) : (
            <Surface style={styles.placeholder} elevation={0}>
              <IconButton icon="camera-plus-outline" size={32} />
              <Text variant="bodySmall">Toque para adicionar foto</Text>
            </Surface>
          )}
          <Button mode="text" onPress={pickImage} style={styles.pickButton}>
            {image ? 'Trocar Imagem' : 'Selecionar Imagem'}
          </Button>
        </View>

        <Button
          mode="contained"
          onPress={handleSave}
          disabled={uploading}
          style={styles.saveButton}
          contentStyle={styles.saveButtonContent}
          buttonColor={theme.colors.secondary}
          textColor="#000"
        >
          {uploading ? <ActivityIndicator color="#000" /> : 'SALVAR PRODUTO'}
        </Button>
      </Surface>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  scrollContent: {
    padding: 16,
  },
  card: {
    padding: 16,
    borderRadius: 20,
    backgroundColor: '#FFFFFF',
  },
  input: {
    marginBottom: 16,
    backgroundColor: '#FFFFFF',
  },
  outline: {
    borderRadius: 12,
  },
  label: {
    marginBottom: 8,
    fontWeight: '600',
    color: '#495057',
  },
  categoriesContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 16,
  },
  chip: {
    margin: 4,
  },
  imageContainer: {
    alignItems: 'center',
    marginBottom: 24,
  },
  preview: {
    width: '100%',
    height: 180,
    borderRadius: 12,
  },
  placeholder: {
    width: '100%',
    height: 180,
    borderRadius: 12,
    backgroundColor: '#F1F3F5',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#DEE2E6',
    borderStyle: 'dashed',
  },
  pickButton: {
    marginTop: 4,
  },
  saveButton: {
    borderRadius: 12,
  },
  saveButtonContent: {
    height: 54,
  },
});
