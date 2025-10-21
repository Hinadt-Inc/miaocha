import type { Rule } from 'antd/es/form';

// 必填
export const required = (text = '', isRequired = true) => ({
  required: isRequired,
  message: `${text}必填`,
});

// 最大字符数
export const max = (max: number) => ({ max, message: `不超过${max}个字符！` });
// 最小字符数
export const min = (min: number) => ({ min, message: `不少于${min}个字符！` });

// 禁止包含任何空白字符（首尾、中间都不允许）：空格、制表符、换行等
export const noWhitespace = (message = '不能包含空格！'): Rule => ({
  validator: (_rule, value) => {
    // 交给 required 规则处理“必填”，这里不拦空值
    if (value == null || value === '') return Promise.resolve();

    const str = String(value);
    // \s 会匹配所有空白字符（space、tab、CR/LF等）
    return /\s/.test(str) ? Promise.reject(new Error(message)) : Promise.resolve();
  },
});

// 密码策略：长度范围 + 必须包含字母和数字
export const passwordPolicy = (
  min = 6,
  max = 20,
  lengthMessage = `长度需在${min}~${max}个字符之间`,
  compositionMessage = '必须包含字母和数字',
  whitespaceMessage = '不能包含空格！', // 这里的“空格”文案可根据需求调整
  // 如果你只想拦普通空格而允许 Tab/换行，把这个改为 'spaceOnly'
  whitespaceMode: 'anyWhitespace' | 'spaceOnly' = 'anyWhitespace',
): Rule => ({
  validator: (_rule, value) => {
    // 把“必填”交给 required 规则，这里不拦空值
    if (value == null || value === '') return Promise.resolve();

    const str = String(value);

    // 禁止空白字符（严格：\s；或仅普通空格）
    const whitespaceRegex = whitespaceMode === 'anyWhitespace' ? /\s/ : / /;
    if (whitespaceRegex.test(str)) {
      return Promise.reject(new Error(whitespaceMessage));
    }

    // 长度校验
    if (str.length < min || str.length > max) {
      return Promise.reject(new Error(lengthMessage));
    }

    // 组成校验：至少一个字母 + 至少一个数字
    if (!/^(?=.*[A-Za-z])(?=.*\d).+$/.test(str)) {
      return Promise.reject(new Error(compositionMessage));
    }

    return Promise.resolve();
  },
});
