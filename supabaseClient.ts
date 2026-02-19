
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = (import.meta as any).env?.VITE_SUPABASE_URL || 'https://zlykhkpycrsukoaxhfzn.supabase.co';
const supabaseKey = (import.meta as any).env?.VITE_SUPABASE_ANON_KEY || 'sb_publishable_F9BmcR4Fv39SK1Kiz3yKFQ_75DYBudY';

export const supabase = createClient(supabaseUrl, supabaseKey);
