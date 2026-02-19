
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = 'https://zlykhkpycrsukoaxhfzn.supabase.co';
const supabaseKey = 'sb_publishable_F9BmcR4Fv39SK1Kiz3yKFQ_75DYBudY';

export const supabase = createClient(supabaseUrl, supabaseKey);
